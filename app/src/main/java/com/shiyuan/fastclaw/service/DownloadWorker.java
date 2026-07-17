package com.shiyuan.fastclaw.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ForegroundInfo;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.blankj.utilcode.util.AppUtils;
import com.nhsd.basic.bus.AppEvent;
import com.nhsd.basic.bus.FlowEventBus;
import com.nhsd.basic.manager.FileManager;
import com.shiyuan.fastclaw.R;
import com.shiyuan.fastclaw.common.utils.FileUtils;
import com.shiyuan.fastclaw.core.Events;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 使用 WorkManager 执行文件下载。
 */
public class DownloadWorker extends Worker {

    public static final String UPDATE_FILE_NAME = "appUpgrade.apk";
    public static final String DOWNLOAD_TYPE_TASK = "download_type_task";
    public static final String DOWNLOAD_TYPE_CONTRACT = "download_type_contract";

    private static final String KEY_DOWNLOAD_URL = "download_url";
    private static final String KEY_MD5_HASH = "md5_hash";
    private static final String KEY_FILE_NAME = "file_name";
    private static final String KEY_DOWNLOAD_WITH_NOTIFY = "download_with_notify";
    private static final String KEY_DOWNLOAD_TYPE = "download_type";
    private static final String KEY_PROGRESS = "progress";
    private static final String KEY_OUTPUT_PATH = "path";
    private static final String WORK_NAME_PREFIX = "file_download_";
    private static final String WORK_TAG = "file_download";
    private static final String NOTIFICATION_CHANNEL_ID = "file_download";
    private static final String TEMP_FILE_SUFFIX = ".download";
    private static final int MAX_RETRY_COUNT = 2;
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile Call currentCall;
    private volatile File tempFile;

    public DownloadWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    public static void enqueue(Context context, String url, String fileName) {
        enqueue(context, url, null, fileName, false, null);
    }

    public static void enqueue(
            Context context,
            String url,
            String md5,
            String fileName,
            boolean downloadWithNotify,
            String downloadType
    ) {
        if (context == null || TextUtils.isEmpty(url)) {
            return;
        }

        String requestedFileName = TextUtils.isEmpty(fileName) ? UPDATE_FILE_NAME : fileName;
        String targetFileName = new File(requestedFileName).getName();
        if (TextUtils.isEmpty(targetFileName)) {
            targetFileName = UPDATE_FILE_NAME;
        }
        Data.Builder inputData = new Data.Builder()
                .putString(KEY_DOWNLOAD_URL, url)
                .putString(KEY_FILE_NAME, targetFileName)
                .putBoolean(KEY_DOWNLOAD_WITH_NOTIFY, downloadWithNotify);
        if (!TextUtils.isEmpty(md5)) {
            inputData.putString(KEY_MD5_HASH, md5);
        }
        if (!TextUtils.isEmpty(downloadType)) {
            inputData.putString(KEY_DOWNLOAD_TYPE, downloadType);
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DownloadWorker.class)
                .setInputData(inputData.build())
                .setConstraints(constraints)
                .addTag(WORK_TAG)
                .build();

        String uniqueWorkName = WORK_NAME_PREFIX + targetFileName;
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, request);
    }

    @NonNull
    @Override
    public Result doWork() {
        String url = getInputData().getString(KEY_DOWNLOAD_URL);
        String fileName = getInputData().getString(KEY_FILE_NAME);
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(fileName)) {
            return Result.failure();
        }

        String md5 = getInputData().getString(KEY_MD5_HASH);
        boolean downloadWithNotify = getInputData().getBoolean(KEY_DOWNLOAD_WITH_NOTIFY, false);
        String downloadType = getInputData().getString(KEY_DOWNLOAD_TYPE);

        try {
            setForegroundAsync(createForegroundInfo(fileName, 0)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.failure();
        } catch (CancellationException e) {
            return Result.failure();
        } catch (ExecutionException e) {
            notifyDownloadFailed(downloadWithNotify, fileName);
            return Result.failure();
        }

        File targetFile = new File(FileManager.getInstance().getApkFolder(), fileName);

        if (isValidExistingFile(targetFile, md5)) {
            notifyDownloadCompleted(url, targetFile, downloadWithNotify, downloadType);
            return Result.success(createOutputData(targetFile));
        }
        if (!TextUtils.isEmpty(md5) && targetFile.exists() && !targetFile.delete()) {
            notifyDownloadFailed(downloadWithNotify, fileName);
            return Result.failure();
        }

        try {
            download(url, targetFile, fileName);
            if (isStopped()) {
                return Result.failure();
            }
            if (!isValidDownloadedFile(tempFile, md5) || !replaceTargetFile(targetFile)) {
                deleteTempFile();
                notifyDownloadFailed(downloadWithNotify, fileName);
                return Result.failure();
            }

            notifyDownloadCompleted(url, targetFile, downloadWithNotify, downloadType);
            return Result.success(createOutputData(targetFile));
        } catch (HttpStatusException e) {
            deleteTempFile();
            notifyDownloadFailed(downloadWithNotify, fileName);
            return Result.failure();
        } catch (IOException e) {
            if (isStopped()) {
                return Result.failure();
            }
            if (getRunAttemptCount() < MAX_RETRY_COUNT) {
                return Result.retry();
            }
            deleteTempFile();
            notifyDownloadFailed(downloadWithNotify, fileName);
            return Result.failure();
        } catch (IllegalArgumentException e) {
            deleteTempFile();
            notifyDownloadFailed(downloadWithNotify, fileName);
            return Result.failure();
        }
    }

    private void download(String url, File targetFile, String fileName) throws IOException {
        File parent = targetFile.getParentFile();
        if (parent == null || (!parent.exists() && !parent.mkdirs())) {
            throw new IOException("Unable to create download directory");
        }

        tempFile = new File(parent, targetFile.getName() + "." + getId() + TEMP_FILE_SUFFIX);
        deleteStaleTempFiles(parent, targetFile.getName());
        long existingBytes = tempFile.exists() ? tempFile.length() : 0L;

        Request.Builder requestBuilder = new Request.Builder().url(url);
        if (existingBytes > 0L) {
            requestBuilder.header("Range", "bytes=" + existingBytes + "-");
        }
        currentCall = HTTP_CLIENT.newCall(requestBuilder.build());
        try (Response response = currentCall.execute()) {
            if (response.code() == 416 && existingBytes > 0L) {
                deleteTempFile();
                throw new IOException("Requested range is not satisfiable");
            }
            ResponseBody body = response.body();
            if (!response.isSuccessful()) {
                throw new HttpStatusException();
            }
            if (body == null) {
                throw new IOException("Empty response body");
            }

            boolean append = existingBytes > 0L && response.code() == 206;
            if (append && !isExpectedContentRange(response.header("Content-Range"), existingBytes)) {
                deleteTempFile();
                throw new IOException("Unexpected content range");
            }
            if (!append) {
                existingBytes = 0L;
            }
            long responseBytes = body.contentLength();
            long totalBytes = responseBytes >= 0L ? existingBytes + responseBytes : -1L;
            writeResponseBody(body, fileName, url, existingBytes, totalBytes, append);
        } finally {
            currentCall = null;
        }
    }

    private boolean isExpectedContentRange(String contentRange, long existingBytes) {
        String expectedPrefix = "bytes " + existingBytes + "-";
        return contentRange != null
                && contentRange.regionMatches(true, 0, expectedPrefix, 0, expectedPrefix.length());
    }

    private void writeResponseBody(
            ResponseBody body,
            String fileName,
            String url,
            long downloadedBytes,
            long totalBytes,
            boolean append
    ) throws IOException {
        int lastProgress = -1;
        byte[] buffer = new byte[8 * 1024];

        try (InputStream input = body.byteStream();
             FileOutputStream output = new FileOutputStream(tempFile, append)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (isStopped()) {
                    throw new IOException("Download stopped");
                }
                output.write(buffer, 0, read);
                downloadedBytes += read;

                int progress = calculateProgress(downloadedBytes, totalBytes);
                if (progress != lastProgress) {
                    lastProgress = progress;
                    updateProgress(fileName, url, progress);
                }
            }
            output.flush();
        }
    }

    private int calculateProgress(long downloadedBytes, long totalBytes) {
        if (totalBytes <= 0L) {
            return 1;
        }
        return Math.max(1, Math.min(99, (int) (downloadedBytes * 100L / totalBytes)));
    }

    private void updateProgress(String fileName, String url, int progress) {
        setProgressAsync(new Data.Builder().putInt(KEY_PROGRESS, progress).build());
        setForegroundAsync(createForegroundInfo(fileName, progress));
        FlowEventBus.postSync(new Events.DownloadProgressEvent(url, progress, null));
    }

    private boolean isValidExistingFile(File file, String md5) {
        return file.exists()
                && file.isFile()
                && !TextUtils.isEmpty(md5)
                && md5.equalsIgnoreCase(FileUtils.getFileMD5(file));
    }

    private boolean isValidDownloadedFile(File file, String md5) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }
        return TextUtils.isEmpty(md5) || md5.equalsIgnoreCase(FileUtils.getFileMD5(file));
    }

    private boolean replaceTargetFile(File targetFile) {
        if (targetFile.exists() && !targetFile.delete()) {
            return false;
        }
        return tempFile != null && tempFile.renameTo(targetFile);
    }

    private void notifyDownloadCompleted(
            String url,
            File file,
            boolean downloadWithNotify,
            String downloadType
    ) {
        String path = file.getPath();
        if (TextUtils.equals(downloadType, DOWNLOAD_TYPE_CONTRACT)) {
            FlowEventBus.postSync(new Events.DownloadContractEvent(file.getName()));
            showDownloadCompletedNotification(file);
        } else if (downloadWithNotify) {
            FlowEventBus.postSticky(new DownloadApkEvent(path));
            showDownloadCompletedNotification(file);
        } else {
            if (TextUtils.equals(downloadType, DOWNLOAD_TYPE_TASK)) {
                FlowEventBus.postSync(new InstallApkEvent(url, path));
            }
            if (AppUtils.isAppForeground()) {
                mainHandler.post(() -> FileUtils.openFile(getApplicationContext(), file));
            } else {
                showDownloadCompletedNotification(file);
            }
        }
        setProgressAsync(new Data.Builder().putInt(KEY_PROGRESS, 100).build());
        FlowEventBus.postSync(new Events.DownloadProgressEvent(url, 100, path));
    }

    private void notifyDownloadFailed(boolean downloadWithNotify, String fileName) {
        FlowEventBus.postSync(new DownloadFailedEvent());
        if (isStopped()) {
            return;
        }
        if (!downloadWithNotify && AppUtils.isAppForeground()) {
            mainHandler.post(() -> Toast.makeText(
                    getApplicationContext(),
                    R.string.download_failed,
                    Toast.LENGTH_SHORT
            ).show());
        } else {
            showDownloadFailedNotification(fileName);
        }
    }

    private void showDownloadCompletedNotification(File file) {
        Context context = getApplicationContext();
        createNotificationChannel(context);
        PendingIntent openFileIntent = createOpenFilePendingIntent(context, file);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(file.getName())
                .setContentText(context.getString(R.string.download_complete_click_to_open))
                .setAutoCancel(true)
                .setOnlyAlertOnce(true);
        if (openFileIntent != null) {
            builder.setContentIntent(openFileIntent);
        }
        notifySafely(20_000 + (getId().hashCode() & 0x7FFF), builder.build());
    }

    private void showDownloadFailedNotification(String fileName) {
        Context context = getApplicationContext();
        createNotificationChannel(context);
        Notification notification = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(fileName)
                .setContentText(context.getString(R.string.download_failed))
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build();
        notifySafely(30_000 + (getId().hashCode() & 0x7FFF), notification);
    }

    private PendingIntent createOpenFilePendingIntent(Context context, File file) {
        Intent intent;
        try {
            Uri uri = FileProvider.getUriForFile(context, FileManager.getInstance().getFileProvider(), file);
            String mimeType = context.getContentResolver().getType(uri);
            if (TextUtils.isEmpty(mimeType)) {
                String extension = MimeTypeMap.getFileExtensionFromUrl(file.getName());
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
            if (TextUtils.isEmpty(mimeType)) {
                mimeType = "application/octet-stream";
            }
            intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, mimeType)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        } catch (RuntimeException e) {
            intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        }
        if (intent == null) {
            return null;
        }
        return PendingIntent.getActivity(
                context,
                20_000 + (getId().hashCode() & 0x7FFF),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void notifySafely(int notificationId, Notification notification) {
        NotificationManager manager = (NotificationManager) getApplicationContext()
                .getSystemService(Service.NOTIFICATION_SERVICE);
        try {
            manager.notify(notificationId, notification);
        } catch (SecurityException ignored) {
            // 用户关闭通知权限时不影响 Worker 的最终状态。
        }
    }

    private Data createOutputData(File file) {
        return new Data.Builder().putString(KEY_OUTPUT_PATH, file.getPath()).build();
    }

    private ForegroundInfo createForegroundInfo(String fileName, int progress) {
        Context context = getApplicationContext();
        createNotificationChannel(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(fileName)
                .setContentText(context.getString(R.string.downloading_please_wait, progress))
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setProgress(100, progress, progress <= 0);
        builder.addAction(
                android.R.drawable.ic_delete,
                context.getString(android.R.string.cancel),
                WorkManager.getInstance(context).createCancelPendingIntent(getId())
        );
        Notification notification = builder.build();
        int notificationId = 10_000 + (getId().hashCode() & 0x7FFF);
        int foregroundServiceType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                : 0;
        return new ForegroundInfo(notificationId, notification, foregroundServiceType);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = (NotificationManager) context.getSystemService(Service.NOTIFICATION_SERVICE);
        if (manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.download),
                    NotificationManager.IMPORTANCE_LOW
            );
            manager.createNotificationChannel(channel);
        }
    }

    private void deleteTempFile() {
        File file = tempFile;
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    private void deleteStaleTempFiles(File parent, String targetFileName) {
        File[] staleFiles = parent.listFiles(file -> file.isFile()
                && file.getName().startsWith(targetFileName + ".")
                && file.getName().endsWith(TEMP_FILE_SUFFIX)
                && !file.equals(tempFile));
        if (staleFiles == null) {
            return;
        }
        for (File staleFile : staleFiles) {
            staleFile.delete();
        }
    }

    private static class HttpStatusException extends IOException {
        HttpStatusException() {
            super();
        }
    }

    @Override
    public void onStopped() {
        Call call = currentCall;
        if (call != null) {
            call.cancel();
        }
        FlowEventBus.postSync(new DownloadFailedEvent());
        mainHandler.removeCallbacksAndMessages(null);
        super.onStopped();
    }

    public static class DownloadFailedEvent implements AppEvent {
    }

    public static class DownloadApkEvent implements AppEvent {
        public final String path;

        public DownloadApkEvent(String path) {
            this.path = path;
        }
    }

    public static class InstallApkEvent implements AppEvent {
        public final String url;
        public final String path;

        public InstallApkEvent(String url, String path) {
            this.url = url;
            this.path = path;
        }
    }
}

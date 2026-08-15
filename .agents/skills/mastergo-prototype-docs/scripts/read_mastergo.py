#!/usr/bin/env python3
"""Read MasterGo prototype metadata and data without exposing browser cookies."""

from __future__ import annotations

import argparse
import copy
import html
import json
import re
import shutil
import sys
import tempfile
import time
from http.cookiejar import CookieJar, LoadError, MozillaCookieJar
from pathlib import Path
from typing import Any, Optional
from urllib.error import HTTPError, URLError
from urllib.parse import parse_qs, quote, unquote, urlencode, urljoin, urlparse
from urllib.request import (
    HTTPCookieProcessor,
    HTTPRedirectHandler,
    Request,
    build_opener,
)


USER_AGENT = "ProjectDocs-MasterGo-Reader/1.0"
METADATA_LIMIT = 8 * 1024 * 1024
HTML_LIMIT = 4 * 1024 * 1024
SCRIPT_LIMIT = 12 * 1024 * 1024
MAX_SCRIPT_ASSETS = 12
DEFAULT_MIN_DATA_BYTES = 1024
ASSET_PATTERN = re.compile(
    rb"(?:https?://[A-Za-z0-9._~:/?#\[\]@!$&'()*+,;=%-]+|[A-Za-z0-9_./%-]{4,})"
    rb"\.(?:png|jpe?g|webp)(?:\?[A-Za-z0-9._~:/?#\[\]@!$&'()*+,;=%-]*)?",
    re.IGNORECASE,
)


class ReaderError(RuntimeError):
    """Expected read failure with a user-safe message."""


def is_mastergo_host(host: Optional[str]) -> bool:
    value = (host or "").lower().rstrip(".")
    return value == "mastergo.com" or value.endswith(".mastergo.com")


def require_mastergo_url(raw_url: str) -> str:
    parsed = urlparse(raw_url.strip())
    if parsed.scheme != "https" or not is_mastergo_host(parsed.hostname):
        raise ReaderError("只接受 https://mastergo.com 或其子域名链接。")
    return parsed.geturl()


def extract_document_id(raw_url: str) -> str:
    parsed = urlparse(raw_url)
    path_match = re.search(r"/(?:file|prototyping)/(\d+)(?:/|$)", parsed.path)
    if path_match:
        return path_match.group(1)

    query = parse_qs(parsed.query)
    for key in ("documentId", "document_id", "file", "shareId"):
        for value in query.get(key, []):
            if value.isdigit():
                return value
    raise ReaderError("无法从链接解析 documentId。")


def first_value(value: Any, key: str) -> Any:
    if isinstance(value, dict):
        if key in value:
            return value[key]
        for child in value.values():
            found = first_value(child, key)
            if found is not None:
                return found
    elif isinstance(value, list):
        for child in value:
            found = first_value(child, key)
            if found is not None:
                return found
    return None


def extract_wv(page_text: str, metadata: Any, explicit_wv: Optional[str]) -> str:
    if explicit_wv:
        if not re.fullmatch(r"[A-Za-z0-9._-]+", explicit_wv):
            raise ReaderError("--wv 包含不支持的字符。")
        return explicit_wv

    metadata_wv = first_value(metadata, "wv")
    if isinstance(metadata_wv, str) and re.fullmatch(r"[A-Za-z0-9._-]+", metadata_wv):
        return metadata_wv

    decoded = unquote(html.unescape(page_text))
    patterns = (
        r"/data/[^?\"'\s]+\?wv=([A-Za-z0-9._-]+)",
        r"(?:[?&]|\\u0026)wv=([A-Za-z0-9._-]+)",
        r"[\"']wv[\"']\s*[:=]\s*[\"']([A-Za-z0-9._-]+)[\"']",
    )
    for pattern in patterns:
        match = re.search(pattern, decoded)
        if match:
            return match.group(1)
    raise ReaderError("无法从当前页面确认 wv；接口可能变化，可核实后显式传入 --wv。")


def extract_master_version(script_text: str) -> Optional[str]:
    decoded = unquote(html.unescape(script_text))
    matches = {
        match
        for match in re.findall(
            r"[\"']?VUE_APP_MASTER_VERSION[\"']?\s*:\s*[\"']([A-Za-z0-9._-]+)[\"']",
            decoded,
        )
    }
    if len(matches) == 1:
        return next(iter(matches))
    return None


def extract_script_urls(page_text: str, page_url: str) -> list[str]:
    decoded = html.unescape(page_text)
    urls: list[str] = []
    seen: set[str] = set()
    for match in re.finditer(
        r"<script\b[^>]*\bsrc\s*=\s*([\"'])(.*?)\1",
        decoded,
        flags=re.IGNORECASE | re.DOTALL,
    ):
        try:
            candidate = require_mastergo_url(urljoin(page_url, match.group(2).strip()))
        except ReaderError:
            continue
        if not urlparse(candidate).path.lower().endswith(".js") or candidate in seen:
            continue
        urls.append(candidate)
        seen.add(candidate)
        if len(urls) >= MAX_SCRIPT_ASSETS:
            break
    return urls


def extract_wv_from_page_assets(
    page_text: str,
    page_url: str,
    opener: Any,
    timeout: float,
) -> str:
    versions: set[str] = set()
    for script_url in extract_script_urls(page_text, page_url):
        try:
            script_bytes, _, _ = read_limited(
                opener,
                script_url,
                page_url,
                timeout,
                SCRIPT_LIMIT,
            )
        except (HTTPError, URLError, ReaderError):
            continue
        version = extract_master_version(script_bytes.decode("utf-8", errors="ignore"))
        if version:
            versions.add(version)

    if len(versions) == 1:
        return next(iter(versions))
    if len(versions) > 1:
        raise ReaderError("页面运行包中存在多个 MasterGo 数据版本，无法安全选择 wv。")
    raise ReaderError("无法从当前页面或其 MasterGo 运行包确认 wv；可核实后显式传入 --wv。")


def check_cookie_header(path: Path) -> None:
    try:
        first_line = path.read_bytes().splitlines()[0].decode("ascii", errors="replace")
    except (OSError, IndexError):
        raise ReaderError("Cookie Jar 不存在、为空或不可读。")
    if first_line not in {"# Netscape HTTP Cookie File", "# HTTP Cookie File"}:
        raise ReaderError("Cookie Jar 必须使用 Netscape HTTP Cookie File 格式。")


def load_mastergo_cookies(path: Path) -> CookieJar:
    check_cookie_header(path)
    source = MozillaCookieJar(str(path))
    try:
        source.load(ignore_discard=True, ignore_expires=False)
    except (LoadError, OSError):
        raise ReaderError("Cookie Jar 无法按 Netscape 格式解析。")

    filtered = CookieJar()
    now = time.time()
    for cookie in source:
        if is_mastergo_host(cookie.domain.lstrip(".")) and not cookie.is_expired(now):
            filtered.set_cookie(copy.copy(cookie))
    if not list(filtered):
        raise ReaderError("Cookie Jar 中没有可用且未过期的 MasterGo Cookie。")
    return filtered


class MasterGoRedirectHandler(HTTPRedirectHandler):
    def redirect_request(
        self,
        req: Request,
        fp: Any,
        code: int,
        msg: str,
        headers: Any,
        newurl: str,
    ) -> Optional[Request]:
        resolved = urljoin(req.full_url, newurl)
        require_mastergo_url(resolved)
        return super().redirect_request(req, fp, code, msg, headers, resolved)


def make_opener(cookie_jar: Optional[CookieJar]) -> Any:
    handlers: list[Any] = [MasterGoRedirectHandler()]
    if cookie_jar is not None:
        handlers.append(HTTPCookieProcessor(cookie_jar))
    return build_opener(*handlers)


def request_headers(referer: str) -> dict[str, str]:
    return {
        "User-Agent": USER_AGENT,
        "MGVersion": "1.0",
        "lang": "zh-CN",
        "Referer": referer,
    }


def read_limited(opener: Any, url: str, referer: str, timeout: float, limit: int) -> tuple[bytes, int, str]:
    require_mastergo_url(url)
    request = Request(url, headers=request_headers(referer), method="GET")
    with opener.open(request, timeout=timeout) as response:
        final_url = require_mastergo_url(response.geturl())
        data = response.read(limit + 1)
        if len(data) > limit:
            raise ReaderError("响应超过安全读取上限。")
        return data, int(response.getcode() or 0), final_url


def download_data(
    opener: Any,
    url: str,
    referer: str,
    timeout: float,
    target: Path,
    minimum_bytes: int,
) -> tuple[int, int]:
    require_mastergo_url(url)
    request = Request(url, headers=request_headers(referer), method="GET")
    with opener.open(request, timeout=timeout) as response, target.open("wb") as output:
        require_mastergo_url(response.geturl())
        status = int(response.getcode() or 0)
        content_type = (response.headers.get("Content-Type") or "").lower()
        first_chunk = b""
        total = 0
        while True:
            chunk = response.read(1024 * 1024)
            if not chunk:
                break
            if not first_chunk:
                first_chunk = chunk[:1024]
            output.write(chunk)
            total += len(chunk)

    looks_like_error = first_chunk.lstrip().startswith((b"{", b"<"))
    if status != 200 or total < minimum_bytes or "text/" in content_type or looks_like_error:
        target.unlink(missing_ok=True)
        raise ReaderError("data 包状态、大小或内容异常。")
    return status, total


def clean_context(raw: bytes) -> str:
    text = raw.decode("utf-8", errors="ignore")
    text = re.sub(r"[\x00-\x1f\x7f]+", " ", text)
    return re.sub(r"\s+", " ", text).strip()


def extract_keyword_matches(data: bytes, keywords: list[str], limit: int) -> list[dict[str, str]]:
    matches: list[dict[str, str]] = []
    seen: set[tuple[str, str]] = set()
    searches: list[tuple[str, bytes]] = []
    seen_keywords: set[str] = set()
    for keyword in keywords:
        needle = keyword.encode("utf-8")
        if not needle or keyword in seen_keywords:
            continue
        searches.append((keyword, needle))
        seen_keywords.add(keyword)

    positions = [0] * len(searches)
    exhausted = [False] * len(searches)
    while len(matches) < limit:
        found_in_round = False
        for index, (keyword, needle) in enumerate(searches):
            if exhausted[index]:
                continue
            found = data.find(needle, positions[index])
            if found < 0:
                exhausted[index] = True
                continue
            found_in_round = True
            snippet = clean_context(data[max(0, found - 180) : found + len(needle) + 260])
            key = (keyword, snippet)
            if snippet and key not in seen:
                matches.append({"keyword": keyword, "context": snippet[:500]})
                seen.add(key)
                if len(matches) >= limit:
                    break
            positions[index] = found + len(needle)
        if not found_in_round:
            break
    return matches


def extract_asset_paths(data: bytes, limit: int) -> list[str]:
    assets: list[str] = []
    seen: set[str] = set()
    for match in ASSET_PATTERN.finditer(data):
        value = match.group(0).decode("ascii", errors="ignore")
        if value and value not in seen:
            assets.append(value[:1000])
            seen.add(value)
        if len(assets) >= limit:
            break
    return assets


def safe_metadata_summary(metadata: Any) -> dict[str, Any]:
    summary: dict[str, Any] = {}
    for key in (
        "id",
        "name",
        "originDoc",
        "isPublic",
        "allowCopy",
        "visitControlPerm",
        "roleName",
        "fileKey",
    ):
        value = first_value(metadata, key)
        if isinstance(value, (str, int, float, bool)) or value is None:
            summary[key] = value
    rights_local_mcp = first_value(metadata, "localMcp")
    if isinstance(rights_local_mcp, (str, int, float, bool)) or rights_local_mcp is None:
        summary["rights.localMcp"] = rights_local_mcp
    return summary


def read_mastergo(args: argparse.Namespace) -> Path:
    source_url = require_mastergo_url(args.url)
    document_id = extract_document_id(source_url)
    cookie_jar = load_mastergo_cookies(Path(args.cookie_jar)) if args.cookie_jar else None
    opener = make_opener(cookie_jar)
    output_dir: Optional[Path] = None

    try:
        page_bytes, page_status, resolved_url = read_limited(
            opener, source_url, source_url, args.timeout, HTML_LIMIT
        )
        page_text = page_bytes.decode("utf-8", errors="ignore")

        metadata_url = (
            f"https://mastergo.com/api/v1/documents/{document_id}?"
            + urlencode({"pageName": "file"})
        )
        metadata_bytes, metadata_status, _ = read_limited(
            opener, metadata_url, resolved_url, args.timeout, METADATA_LIMIT
        )
        try:
            metadata = json.loads(metadata_bytes.decode("utf-8-sig"))
        except (UnicodeDecodeError, json.JSONDecodeError):
            raise ReaderError("metadata 不是有效 JSON。")

        file_key = first_value(metadata, "fileKey")
        if not isinstance(file_key, str) or not file_key:
            raise ReaderError("metadata 中缺少 fileKey，可能没有权限或接口已变化。")
        if args.wv:
            wv = extract_wv(page_text, metadata, args.wv)
        else:
            try:
                wv = extract_wv(page_text, metadata, None)
            except ReaderError:
                wv = extract_wv_from_page_assets(
                    page_text,
                    resolved_url,
                    opener,
                    args.timeout,
                )

        output_dir = Path(tempfile.mkdtemp(prefix=f"mastergo-{document_id}-"))
        data_path = output_dir / "mastergo.data"
        data_url = (
            f"https://mastergo.com/data/{quote(file_key, safe='')}?"
            + urlencode({"wv": wv})
        )
        data_status, data_size = download_data(
            opener,
            data_url,
            resolved_url,
            args.timeout,
            data_path,
            args.min_data_bytes,
        )

        data = data_path.read_bytes()
        result = {
            "document_id": document_id,
            "auth_mode": "cookie-jar" if cookie_jar is not None else "anonymous",
            "page_http_status": page_status,
            "metadata_http_status": metadata_status,
            "metadata_code": metadata.get("code") if isinstance(metadata, dict) else None,
            "metadata": safe_metadata_summary(metadata),
            "wv": wv,
            "data_http_status": data_status,
            "data_size": data_size,
            "keyword_matches": extract_keyword_matches(data, args.keyword, args.max_matches),
            "asset_paths": extract_asset_paths(data, args.max_assets),
        }
        result_path = output_dir / "result.json"
        result_path.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
        return result_path
    except Exception:
        if output_dir is not None:
            shutil.rmtree(output_dir, ignore_errors=True)
        raise


def run_self_test() -> None:
    sample_url = "https://mastergo.com/file/123456?page_id=1%3A2&shareId=123456"
    assert require_mastergo_url(sample_url) == sample_url
    assert extract_document_id(sample_url) == "123456"
    assert extract_document_id("https://mastergo.com/view?documentId=987") == "987"
    assert extract_wv("fetch('/data/key?wv=v2.4.1')", {}, None) == "v2.4.1"
    assert extract_master_version('VUE_APP_MASTER_VERSION:"v2.191.0"') == "v2.191.0"
    script_urls = extract_script_urls(
        '<script src="https://static.mastergo.com/static/js/app.test.js"></script>',
        "https://mastergo.com/prototyping/123",
    )
    assert script_urls == ["https://static.mastergo.com/static/js/app.test.js"]
    assert first_value({"data": {"fileKey": "abc"}}, "fileKey") == "abc"
    matches = extract_keyword_matches("页面状态：空态".encode("utf-8"), ["状态"], 5)
    assert matches and matches[0]["keyword"] == "状态"
    balanced = extract_keyword_matches(
        "高频 高频 高频 稀有".encode("utf-8"),
        ["高频", "稀有"],
        2,
    )
    assert {match["keyword"] for match in balanced} == {"高频", "稀有"}
    assets = extract_asset_paths(b"a/b/cover.webp and https://example.com/x.png", 5)
    assert "a/b/cover.webp" in assets
    assert not is_mastergo_host("example.com")
    print("Self-test passed.")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="安全读取 MasterGo metadata/data，并限量提取关键词和图片路径。"
    )
    parser.add_argument("url", nargs="?", help="MasterGo 原型链接")
    parser.add_argument("--cookie-jar", help="本地 Netscape Cookie Jar 路径")
    parser.add_argument("--check-cookie-jar", help="仅检查本地 Cookie Jar，不联网")
    parser.add_argument("--keyword", action="append", default=[], help="需要限量定位的业务关键词，可重复")
    parser.add_argument("--wv", help="仅在页面无法自动确认时显式提供")
    parser.add_argument("--timeout", type=float, default=30.0, help="单次网络请求超时秒数")
    parser.add_argument("--min-data-bytes", type=int, default=DEFAULT_MIN_DATA_BYTES)
    parser.add_argument("--max-matches", type=int, default=40)
    parser.add_argument("--max-assets", type=int, default=100)
    parser.add_argument("--self-test", action="store_true", help="运行无网络自测")
    return parser


def main() -> int:
    args = build_parser().parse_args()
    try:
        if args.self_test:
            run_self_test()
            return 0
        if args.check_cookie_jar:
            jar = load_mastergo_cookies(Path(args.check_cookie_jar))
            print(f"Cookie Jar valid: {len(list(jar))} usable MasterGo cookie(s).")
            return 0
        if not args.url:
            raise ReaderError("请提供 MasterGo 链接，或使用 --check-cookie-jar / --self-test。")
        if args.timeout <= 0 or args.min_data_bytes <= 0 or args.max_matches < 0 or args.max_assets < 0:
            raise ReaderError("超时、大小和数量参数必须使用有效正数；数量上限可为 0。")

        result_path = read_mastergo(args)
        result = json.loads(result_path.read_text(encoding="utf-8"))
        print("MasterGo read succeeded.")
        print(f"document_id: {result['document_id']}")
        print(f"auth_mode: {result['auth_mode']}")
        print(f"data_size: {result['data_size']}")
        print(f"keyword_matches: {len(result['keyword_matches'])}")
        print(f"asset_paths: {len(result['asset_paths'])}")
        print(f"result: {result_path}")
        return 0
    except ReaderError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2
    except HTTPError as exc:
        print(f"ERROR: MasterGo 返回 HTTP {exc.code}。", file=sys.stderr)
        return 3
    except URLError as exc:
        print(f"ERROR: 无法连接 MasterGo：{exc.reason}", file=sys.stderr)
        return 4
    except OSError as exc:
        print(f"ERROR: 本地文件操作失败：{exc.strerror or 'unknown error'}", file=sys.stderr)
        return 5


if __name__ == "__main__":
    raise SystemExit(main())

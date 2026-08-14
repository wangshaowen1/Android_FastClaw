[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$validatorPath = Join-Path $PSScriptRoot "validate_agent_docs.py"

function Invoke-ValidatorWithPython([string]$CommandName, [string[]]$PrefixArgs) {
    $command = Get-Command $CommandName -ErrorAction SilentlyContinue
    if ($null -eq $command) { return }

    & $command.Source @PrefixArgs -c "import sys; raise SystemExit(0 if sys.version_info >= (3, 9) else 1)" *> $null
    if ($LASTEXITCODE -ne 0) { return }

    & $command.Source @PrefixArgs $validatorPath
    exit $LASTEXITCODE
}

Invoke-ValidatorWithPython "py" @("-3")
Invoke-ValidatorWithPython "python3" @()
Invoke-ValidatorWithPython "python" @()

throw "Python 3.9 or newer was not found. Run the validator with: python .agents/scripts/validate_agent_docs.py"

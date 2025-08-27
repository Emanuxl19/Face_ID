# load-env.ps1
Get-Content .env | ForEach-Object {
    if ($_ -match "^(.*?)=(.*)$") {
        Set-Item -Path "Env:\$($matches[1])" -Value "$($matches[2])"
    }
}

# 玺记记账：在线版 -> 离线版 数据导出脚本（5.8，见 openspec design.md D10）
#
# 用途：从服务端 MySQL 导出交易/分类/预算明文数据，产出 xiji_backup.json，
#       传到手机后由离线版 App「从文件导入」灌入本地库。
#
# 用法示例（PowerShell，mysql.exe 需在 PATH）：
#   .\scripts\export_offline_migration.ps1 -DbPassword "你的root密码" `
#       -Database xiji -OutFile D:\xiji_backup.json
#
# 导出口径（与 App 导入器约定，勿单方面修改字段名）：
#   - 交易：仅 deleted=0 的活数据；family_id/created_by/platform 原样导出，
#     created_at/updated_by 丢弃（业务时间走 date，更新仲裁走 version+updatedAt）
#   - 分类：deleted=0 且 status=1；builtin=true 为系统内置（family_id IS NULL），
#     App 端按 name+type 映射本地固定 UUID，不插入
#   - 预算：家庭级单条（服务端无年月维度），App 端灌入当月
param(
    [string]$DbHost = "localhost",
    [int]$Port = 3306,
    [string]$DbUser = "root",
    [Parameter(Mandatory = $true)]
    [string]$DbPassword,
    [string]$Database = "xiji",
    [string]$OutFile = "xiji_backup.json"
)

$ErrorActionPreference = "Stop"

# mysql.exe 定位：PATH 优先，找不到提示常见安装路径
$mysql = Get-Command mysql -ErrorAction SilentlyContinue
if (-not $mysql) {
    $candidates = @(
        "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
        "C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe"
    ) | Where-Object { Test-Path $_ }
    if ($candidates) { $mysql = $candidates[0] } else {
        throw "mysql.exe 未找到：请安装 MySQL 客户端或将 bin 目录加入 PATH"
    }
} else { $mysql = $mysql.Source }

# 密码经环境变量传递，避免命令行明文（也规避 PS 转义问题）
$env:MYSQL_PWD = $DbPassword

function Invoke-MySqlRows {
    param([string]$Sql)
    # -N 去表头；--batch 每行原样输出；utf8mb4 保证中文不乱码
    $tmp = [IO.Path]::GetTempFileName()
    & $mysql -h $DbHost -P $Port -u $DbUser --default-character-set=utf8mb4 `
        -N --batch -e $Sql $Database > $tmp 2>&1
    if ($LASTEXITCODE -ne 0) {
        Remove-Item $tmp -ErrorAction SilentlyContinue
        throw "mysql 查询失败：$Sql"
    }
    $rows = [IO.File]::ReadAllLines($tmp, [Text.Encoding]::UTF8) |
        Where-Object { $_ -ne "" }
    Remove-Item $tmp -ErrorAction SilentlyContinue
    return ,@($rows)
}

Write-Host "导出交易（deleted=0）..."
$txRows = Invoke-MySqlRows @"
SELECT JSON_OBJECT(
  'id', id, 'familyId', family_id, 'type', type, 'amount', amount,
  'categoryId', category_id, 'payMethod', pay_method, 'date', date,
  'description', description, 'platform', platform,
  'counterparty', counterparty, 'merchantOrderNo', merchant_order_no,
  'tradeNo', trade_no, 'createdBy', created_by, 'updatedAt', updated_at
) FROM transactions WHERE deleted = 0 ORDER BY date, id;
"@

Write-Host "导出分类（deleted=0 且 status=1）..."
$catRows = Invoke-MySqlRows @"
SELECT JSON_OBJECT(
  'id', id, 'name', name, 'icon', icon, 'type', type,
  'sortOrder', sort_order, 'builtin', family_id IS NULL
) FROM categories WHERE deleted = 0 AND status = 1 ORDER BY type, sort_order;
"@

Write-Host "导出预算（deleted=0）..."
$budgetRows = Invoke-MySqlRows @"
SELECT JSON_OBJECT(
  'familyId', family_id, 'amount', amount
) FROM budgets WHERE deleted = 0;
"@

# 拼 JSON 文档（version 字段为导入器格式校验依据）
$stamp = (Get-Date).ToString("yyyy-MM-dd'T'HH:mm:ss")
$json = @()
$json += "{"
$json += "  `"version`": 1,"
$json += "  `"exportedAt`": `"$stamp`","
$json += "  `"categories`": [" + (($catRows | ForEach-Object { "    $_" }) -join ",`n") + "],"
$json += "  `"transactions`": [" + (($txRows | ForEach-Object { "    $_" }) -join ",`n") + "],"
$json += "  `"budgets`": [" + (($budgetRows | ForEach-Object { "    $_" }) -join ",`n") + "]"
$json += "}"

# 自校验：拼出的文档必须能被解析
try { $null = $json -join "`n" | ConvertFrom-Json } catch {
    throw "生成的 JSON 无法解析，请把本输出发给开发排查：$($_.Exception.Message)"
}

# UTF-8 无 BOM 写出（App 端按 UTF-8 解码）
$enc = New-Object Text.UTF8Encoding($false)
[IO.File]::WriteAllText((Join-Path (Get-Location) $OutFile), ($json -join "`n"), $enc)
Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "导出完成：$OutFile"
Write-Host ("  交易   {0} 条" -f @($txRows).Count)
Write-Host ("  分类   {0} 条（其中内置 {1} 条）" -f @($catRows).Count, @($catRows | Where-Object { $_ -match '"builtin": ?true' }).Count)
Write-Host ("  预算   {0} 条" -f @($budgetRows).Count)
Write-Host "传到手机后：我的页 -> 从备份文件导入"

# 将 admin-web 中被裁剪模块的目录移动到 .archive-admin-web 备份(可恢复,迁移完成后可删除)
$ErrorActionPreference = 'Continue'
$root = 'D:\MyProjects\TenderIntelligencePlatform'

$apiMods = @('crm','erp','fms','hrm','im','iot','mall','member','mes','mp','oa','pay','pms','wms')
$viewsMods = @('bpm','crm','erp','fms','hrm','im','iot','mall','member','mes','mp','oa','pay','pms','report','wms')
$compMods = @('DiyEditor','bpmnProcessDesigner','SimpleProcessDesignerV2')

foreach ($m in $apiMods) {
  $src = Join-Path $root "admin-web\src\api\$m"
  $dst = Join-Path $root ".archive-admin-web\api\$m"
  if (Test-Path $src) {
    Move-Item -Path $src -Destination $dst -Force
    Write-Host "moved api/$m"
  } else {
    Write-Host "skip api/$m (not exists)"
  }
}
foreach ($m in $viewsMods) {
  $src = Join-Path $root "admin-web\src\views\$m"
  $dst = Join-Path $root ".archive-admin-web\views\$m"
  if (Test-Path $src) {
    Move-Item -Path $src -Destination $dst -Force
    Write-Host "moved views/$m"
  } else {
    Write-Host "skip views/$m (not exists)"
  }
}
foreach ($c in $compMods) {
  $src = Join-Path $root "admin-web\src\components\$c"
  $dst = Join-Path $root ".archive-admin-web\components\$c"
  if (Test-Path $src) {
    Move-Item -Path $src -Destination $dst -Force
    Write-Host "moved components/$c"
  } else {
    Write-Host "skip components/$c (not exists)"
  }
}
Write-Host "DONE"

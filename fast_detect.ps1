Add-Type -AssemblyName System.Drawing

$img = [System.Drawing.Bitmap]::FromFile("C:\Users\theob\.gemini\antigravity-ide\brain\e3445c47-f609-412a-b70f-df1770bcaa46\.user_uploaded\media_1787603766045.png")
$w = $img.Width
$h = $img.Height

# Fast detection: lock bitmap bits or sample on a 10px grid
$circles = @()
$rect = New-Object System.Drawing.Rectangle(0, 0, $w, $h)
$data = $img.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$stride = $data.Stride
$bytes = New-Object byte[] ($stride * $h)
[System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
$img.UnlockBits($data)

# Helper function to get pixel fast
function IsDark($x, $y) {
    if ($x -lt 0 -or $x -ge $w -or $y -lt 0 -or $y -ge $h) { return $false }
    $idx = $y * $stride + $x * 4
    $b = $bytes[$idx]; $g = $bytes[$idx+1]; $r = $bytes[$idx+2]
    return ($r -lt 80 -and $g -lt 80 -and $b -lt 80)
}

# Scan for circle centers: looking for points where ring of radius R is dark and center is white
$candidates = @()
for ($y = 30; $y -lt ($h - 30); $y += 6) {
    for ($x = 30; $x -lt ($w - 30); $x += 6) {
        $idx = $y * $stride + $x * 4
        $cb = $bytes[$idx]; $cg = $bytes[$idx+1]; $cr = $bytes[$idx+2]
        if ($cr -gt 220 -and $cg -gt 220 -and $cb -gt 220) {
            # Test circle ring radii from 25 to 55
            foreach ($r in @(28, 35, 45, 55)) {
                $darkCount = 0
                for ($a = 0; $a -lt 360; $a += 30) {
                    $rad = $a * [Math]::PI / 180.0
                    $px = [int]($x + $r * [Math]::Cos($rad))
                    $py = [int]($y + $r * [Math]::Sin($rad))
                    if (IsDark $px $py) { $darkCount++ }
                }
                if ($darkCount -ge 8) {
                    $candidates += @{ x=$x; y=$y; r=$r }
                    break
                }
            }
        }
    }
}

# Cluster candidates within 30px
$clustered = @()
foreach ($c in $candidates) {
    $existing = $clustered | Where-Object { [Math]::Sqrt([Math]::Pow($_.x - $c.x, 2) + [Math]::Pow($_.y - $c.y, 2)) -lt 35 }
    if (-not $existing) {
        $clustered += @{ x=$c.x; y=$c.y; r=$c.r }
    }
}

Write-Output "Detected $($clustered.Count) circle centers"
$clustered = $clustered | Sort-Object { $_.y * 1000 + $_.x }
$clustered | ForEach-Object { "$($_.x), $($_.y)" }
$img.Dispose()

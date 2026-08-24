Add-Type -AssemblyName System.Drawing

$img = [System.Drawing.Bitmap]::FromFile("C:\Users\theob\.gemini\antigravity-ide\brain\e3445c47-f609-412a-b70f-df1770bcaa46\.user_uploaded\media_1787603766045.png")
$w = $img.Width
$h = $img.Height

# Let's find all circular regions by finding white centers surrounded by black rings
# We sample grid points that are white, check if bounded by black ring within radius ~25-50px
$visited = New-Object 'bool[,]' $w, $h
$circles = @()

for ($y = 20; $y -lt ($h - 20); $y += 5) {
    for ($x = 20; $x -lt ($w - 20); $x += 5) {
        $c = $img.GetPixel($x, $y)
        # Check if inside circle center (white/light)
        if ($c.R -gt 200 -and $c.G -gt 200 -and $c.B -gt 200 -and -not $visited[$x, $y]) {
            # Flood fill or check bounding box of this white component inside black ring
            $minX = $x; $maxX = $x; $minY = $y; $maxY = $y
            $queue = New-Object System.Collections.Generic.Queue[System.Drawing.Point]
            $queue.Enqueue((New-Object System.Drawing.Point($x, $y)))
            $visited[$x, $y] = $true
            $pixelCount = 0
            
            while ($queue.Count -gt 0) {
                $p = $queue.Dequeue()
                $pixelCount++
                if ($p.X -lt $minX) { $minX = $p.X }
                if ($p.X -gt $maxX) { $maxX = $p.X }
                if ($p.Y -lt $minY) { $minY = $p.Y }
                if ($p.Y -gt $maxY) { $maxY = $p.Y }
                
                # Check neighbors
                $dx = @(1, -1, 0, 0)
                $dy = @(0, 0, 1, -1)
                for ($i = 0; $i -lt 4; $i++) {
                    $nx = $p.X + $dx[$i] * 3
                    $ny = $p.Y + $dy[$i] * 3
                    if ($nx -ge 0 -and $nx -lt $w -and $ny -ge 0 -and $ny -lt $h) {
                        if (-not $visited[$nx, $ny]) {
                            $nc = $img.GetPixel($nx, $ny)
                            # Stop at black border or green/red line
                            $isBlack = ($nc.R -lt 80 -and $nc.G -lt 80 -and $nc.B -lt 80)
                            if (-not $isBlack) {
                                $visited[$nx, $ny] = $true
                                $queue.Enqueue((New-Object System.Drawing.Point($nx, $ny)))
                            }
                        }
                    }
                }
            }
            
            $cw = $maxX - $minX
            $ch = $maxY - $minY
            if ($cw -ge 20 -and $ch -ge 20 -and $cw -le 150 -and $ch -le 150) {
                $cx = [int](($minX + $maxX) / 2)
                $cy = [int](($minY + $maxY) / 2)
                $isBig = ($cw -gt 60 -or $ch -gt 60)
                $circles += @{ x=$cx; y=$cy; w=$cw; h=$ch; isBig=$isBig }
            }
        }
    }
}

Write-Output "Found $($circles.Count) circles"
$circles | ForEach-Object { "$($_.x), $($_.y) (w=$($_.w), h=$($_.h), big=$($_.isBig))" }
$img.Dispose()

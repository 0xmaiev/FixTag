"""Turns a markdown file into HTML for print-to-pdf."""
import sys
from pathlib import Path

import markdown

src, out = Path(sys.argv[1]), Path(sys.argv[2])

body = markdown.markdown(
    src.read_text(encoding="utf-8"),
    extensions=["tables", "fenced_code", "sane_lists"],
)

css = """
@page {
  size: A4;
  margin: 16mm 16mm 18mm 16mm;
  @bottom-right { content: counter(page) " / " counter(pages); font: 8pt 'Segoe UI', Arial, sans-serif; color: #5F6B7D; }
  @bottom-left { content: "FixTag · Protocolo experimental"; font: 8pt 'Segoe UI', Arial, sans-serif; color: #5F6B7D; }
}
html { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
body { font-family: 'Segoe UI', Calibri, Arial, sans-serif; font-size: 9.5pt; line-height: 1.42; color: #1F2733; }
h1 { font-size: 19pt; line-height: 1.15; color: #14213D; margin: 0 0 6pt; }
h2 { font-size: 13.5pt; color: #14213D; border-bottom: 1.5pt solid #E8762B; padding-bottom: 2pt; margin: 16pt 0 6pt; break-after: avoid; }
h3 { font-size: 10.8pt; color: #14213D; margin: 11pt 0 3pt; break-after: avoid; }
p { margin: 0 0 6pt; }
ul, ol { margin: 0 0 6pt; padding-left: 16pt; }
li { margin-bottom: 2pt; }
a { color: #1F5BC6; text-decoration: none; }
code { font-family: Consolas, 'Courier New', monospace; font-size: 8.8pt; background: #F1EEE6; padding: 0 2pt; border-radius: 2pt; }
pre { background: #14213D; color: #E9EDF5; padding: 7pt 9pt; border-radius: 4pt; font-size: 8.4pt; line-height: 1.4; white-space: pre-wrap; break-inside: avoid; margin: 4pt 0 8pt; }
pre code { background: none; color: inherit; padding: 0; font-size: inherit; }
blockquote { margin: 6pt 0 10pt; padding: 5pt 10pt; border-left: 3pt solid #E8762B; background: #FBF3EC; }
blockquote p { margin: 0; }
table { border-collapse: collapse; width: 100%; font-size: 8.8pt; margin: 4pt 0 10pt; }
th { background: #14213D; color: #F4F1EA; text-align: left; font-weight: 600; padding: 3.5pt 5pt; }
td { border-bottom: 0.5pt solid #D9D2C3; padding: 3.5pt 5pt; vertical-align: top; }
tr { break-inside: avoid; }
td code, th code { font-size: 8.2pt; }
th code { background: none; color: inherit; }
td:empty { width: 20%; }
.page-break { break-before: page; }
"""

out.write_text(
    f'<!doctype html><html lang="pt-PT"><head><meta charset="utf-8">'
    f"<title>Protocolo experimental: persistência local e funcionamento offline</title>"
    f"<style>{css}</style></head><body>{body}</body></html>",
    encoding="utf-8",
)

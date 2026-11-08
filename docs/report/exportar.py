"""Relatório: Markdown -> HTML -> PDF, com o Edge sem janela (ou o Chrome, se não houver Edge).

    python3 docs/report/exportar.py           # relatorio.md -> relatorio.html e relatorio.pdf
    python3 docs/report/exportar.py --html    # só o HTML, para rever no browser

Corre no WSL e no Windows. O que falta escrever fica [[entre parênteses retos duplos]]: sai
realçado a amarelo no PDF e listado no terminal, com o número da linha. Uma imagem que ainda não
existe em img/ sai como um retângulo tracejado com o nome do ficheiro.
"""
import html
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

import markdown

HERE = Path(__file__).resolve().parent
SRC = HERE / "relatorio.md"
OUT_HTML = HERE / "relatorio.html"
OUT_PDF = HERE / "relatorio.pdf"
TITLE = "FixTag: relatório do trabalho prático"
FOOTER = "FixTag · Relatório do trabalho prático"

BROWSERS = [
    "/mnt/c/Program Files (x86)/Microsoft/Edge/Application/msedge.exe",  # WSL
    "/mnt/c/Program Files/Microsoft/Edge/Application/msedge.exe",
    r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",     # Windows
    r"C:\Program Files\Microsoft\Edge\Application\msedge.exe",
]

CSS = """
@page {
  size: A4;
  margin: 16mm 16mm 18mm 16mm;
  @bottom-right { content: counter(page) " / " counter(pages); font: 8pt 'Segoe UI', Arial, sans-serif; color: #5F6B7D; }
  @bottom-left { content: "%(footer)s"; font: 8pt 'Segoe UI', Arial, sans-serif; color: #5F6B7D; }
}
@page :first { @bottom-left { content: none; } @bottom-right { content: none; } }
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
table { border-collapse: collapse; width: 100%%; font-size: 8.8pt; margin: 4pt 0 10pt; }
th { background: #14213D; color: #F4F1EA; text-align: left; font-weight: 600; padding: 3.5pt 5pt; }
td { border-bottom: 0.5pt solid #D9D2C3; padding: 3.5pt 5pt; vertical-align: top; }
tr { break-inside: avoid; }
td code, th code { font-size: 8.2pt; }
th code { background: none; color: inherit; }
.page-break { break-before: page; }

/* capa */
.cover { height: 258mm; display: flex; flex-direction: column; break-after: page; }
.cover .uni { font-size: 9.5pt; color: #5F6B7D; }
.cover .uni strong { display: block; color: #14213D; font-size: 10.5pt; letter-spacing: .08em; text-transform: uppercase; margin-bottom: 2pt; }
.cover .middle { margin-top: 62mm; }
.cover .kicker { color: #E8762B; font-weight: 600; font-size: 9pt; letter-spacing: .14em; text-transform: uppercase; }
.cover h1 { font-size: 44pt; letter-spacing: -.01em; margin: 4pt 0 2pt; }
.cover .tagline { font-size: 14pt; color: #3A4556; }
.cover .rule { width: 42mm; height: 3pt; background: #E8762B; margin: 16pt 0 12pt; }
.cover .brief { font-size: 10.5pt; color: #3A4556; }
.cover dl { margin: auto 0 0; display: grid; grid-template-columns: 26mm 1fr; row-gap: 5pt; font-size: 10pt; border-top: 0.5pt solid #D9D2C3; padding-top: 10pt; }
.cover dt { color: #5F6B7D; }
.cover dd { margin: 0; color: #14213D; }

/* índice */
.toc { margin: 0 0 14pt; padding: 8pt 12pt; background: #F7F5F0; border-radius: 4pt; columns: 2; column-gap: 18pt; }
.toc .toctitle { display: block; column-span: all; font-weight: 600; color: #14213D; font-size: 10.5pt; margin-bottom: 4pt; }
.toc ul { list-style: none; padding-left: 0; margin: 0; }
.toc li { margin: 0 0 2pt; break-inside: avoid; }
.toc a { color: #1F2733; }

/* figuras: capturas de ecrã lado a lado, diagramas à largura da página */
/* 4 por linha e a última linha centrada; a altura sai sempre da largura, para não esticar */
.shots { display: flex; flex-wrap: wrap; justify-content: center; gap: 10pt 5mm; margin: 6pt 0 10pt; }
figure { margin: 6pt 0 10pt; break-inside: avoid; }
.shots figure { margin: 0; width: 39mm; }
h3 + p { break-after: avoid; }  /* o parágrafo de uma subsecção fica com as figuras que apresenta */
figure img { display: block; width: 100%%; height: auto; border: 0.5pt solid #D9D2C3; border-radius: 4pt; }
figure.diagram img { border: none; }
figcaption { font-size: 8.4pt; color: #5F6B7D; margin-top: 4pt; text-align: center; }

/* o que falta escrever ou capturar */
.todo { background: #FFF3BF; color: #6B5000; border: 0.6pt dashed #D9A800; border-radius: 2pt; padding: 0 2pt; }
.missing { display: flex; align-items: center; justify-content: center; text-align: center; aspect-ratio: 5 / 11; word-break: break-all;
  border: 1pt dashed #D9A800; background: #FFFBEA; color: #6B5000; font: 8pt Consolas, monospace; border-radius: 4pt; padding: 4pt; }
figure.diagram .missing { aspect-ratio: 2 / 1; }
"""


def convert(text):
    body = markdown.markdown(
        text,
        extensions=["tables", "fenced_code", "sane_lists", "toc"],
        extension_configs={"toc": {"title": "Índice", "toc_depth": "2-2"}},
    )
    body = re.sub(r"\[\[(.+?)\]\]", r'<span class="todo">\1</span>', body)

    missing = []

    def image(match):
        src = html.unescape(match.group(1))
        if (HERE / src).exists():
            return match.group(0)
        missing.append(src)
        return f'<span class="missing">{html.escape(src)}</span>'

    body = re.sub(r'<img[^>]*?src="([^"]+)"[^>]*>', image, body)
    page = (
        f'<!doctype html><html lang="pt-PT"><head><meta charset="utf-8"><title>{TITLE}</title>'
        f"<style>{CSS % {'footer': FOOTER}}</style></head><body>{body}</body></html>"
    )
    return page, missing


def browser():
    for candidate in BROWSERS:
        if Path(candidate).exists():
            return candidate
    for name in ("msedge", "chromium", "chromium-browser", "google-chrome"):
        if shutil.which(name):
            return shutil.which(name)
    return None


def print_pdf(exe):
    # a Windows browser started from WSL needs Windows paths
    on_windows = exe.startswith("/mnt/")

    def path(p):
        if not on_windows:
            return str(p)
        return subprocess.run(["wslpath", "-w", str(p)], capture_output=True, text=True, check=True).stdout.strip()

    # its own profile: with a browser window already open, the job would go to it and print nothing
    profile = HERE / ".perfil-browser"
    url = "file:///" + path(OUT_HTML).replace("\\", "/").lstrip("/")
    before = OUT_PDF.stat().st_mtime if OUT_PDF.exists() else 0
    subprocess.run(
        [exe, "--headless", "--disable-gpu", "--no-first-run", "--no-pdf-header-footer",
         "--print-to-pdf-no-header", f"--user-data-dir={path(profile)}",
         f"--print-to-pdf={path(OUT_PDF)}", url],
        capture_output=True, timeout=180,
    )
    shutil.rmtree(profile, ignore_errors=True)
    return OUT_PDF.exists() and OUT_PDF.stat().st_mtime > before


def main():
    text = SRC.read_text(encoding="utf-8")
    page, missing = convert(text)
    OUT_HTML.write_text(page, encoding="utf-8")
    print(f"HTML: {OUT_HTML}")

    todos = [(n, m) for n, line in enumerate(text.splitlines(), 1) for m in re.findall(r"\[\[(.+?)\]\]", line)]
    if todos:
        print(f"\nPor escrever ({len(todos)}):")
        for n, todo in todos:
            print(f"  linha {n:>3}: {todo[:90]}{'…' if len(todo) > 90 else ''}")
    if missing:
        print(f"\nImagens em falta em {HERE / 'img'} ({len(missing)}): {', '.join(missing)}")

    if "--html" in sys.argv:
        return
    exe = browser()
    if exe is None:
        print("\nSem Edge nem Chrome: abrir o HTML no browser e imprimir para PDF, sem cabeçalhos nem rodapés.")
    elif print_pdf(exe):
        print(f"\nPDF: {OUT_PDF}")
    else:
        print("\nO browser não gerou o PDF. Feche-o e tente outra vez, ou imprima o HTML à mão.")


if __name__ == "__main__":
    main()

# -*- coding: utf-8 -*-
"""
compile_report.py
Main orchestration script:
1. Combines modular sections into PROJECT_REPORT.html
2. Converts HTML to PROJECT_REPORT.pdf via Chrome Headless
3. Generates editable PROJECT_REPORT.docx via python-docx
4. Updates PROJECT_REPORT.md with image links and structured text
"""

import os
import sys
import base64
import subprocess
import re
import docx
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

BASE_DIR = r"E:\project emp\pbl java"
IMG_DIR = os.path.join(BASE_DIR, "images")
HTML_OUT = os.path.join(BASE_DIR, "PROJECT_REPORT.html")
PDF_OUT = os.path.join(BASE_DIR, "PROJECT_REPORT.pdf")
DOCX_OUT = os.path.join(BASE_DIR, "PROJECT_REPORT.docx")

sys.path.append(BASE_DIR)
import sections_prelim
import sections_ch1_ch2
import sections_ch3_ch4
import sections_ch5_ch8

def b64(filename):
    p = os.path.join(IMG_DIR, filename)
    if not os.path.exists(p):
        print("Warning: Image not found:", p)
        return ""
    ext = os.path.splitext(filename)[1].lower().replace(".", "")
    if ext == "jpg": ext = "jpeg"
    with open(p, "rb") as f:
        return f"data:image/{ext};base64," + base64.b64encode(f.read()).decode("utf-8")

print("1. Loading image encodings...")
img_usecase = b64("fig1_use_case_overview.png")
img_storage = b64("fig2_storage_workflow_activity.png")
img_sequence = b64("fig3_sequence_upload_pipeline.png")
img_client_arch = b64("fig4_client_service_arch.png")
img_topology = b64("fig5_component_storage_topology.jpg")
img_er = b64("fig6_database_er_diagram.png")
img_cicd = b64("fig7_cicd_release_pipeline.png")

print("2. Assembling HTML document...")
html_parts = []
html_parts.append("""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>PROJECT REPORT - SecureFileShare</title>
<style>
@page {
    size: A4;
    margin: 20mm 18mm 20mm 20mm;
    @bottom-center {
        content: counter(page);
        font-family: 'Times New Roman', Times, serif;
        font-size: 10pt;
    }
}

body {
    font-family: 'Times New Roman', Times, serif;
    font-size: 11.5pt;
    line-height: 1.45;
    color: #111;
    margin: 0;
    padding: 0;
    text-align: justify;
}

.page-break {
    page-break-after: always;
    break-after: page;
}

.cover-container {
    text-align: center;
    padding-top: 15px;
    height: 90vh;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
}

.double-divider {
    border-top: 3px double #000;
    border-bottom: 1px solid #000;
    height: 4px;
    margin: 14px 0 16px 0;
}

h1.cover-main-title {
    font-size: 18pt;
    font-weight: bold;
    letter-spacing: 1px;
    margin: 8px 0 4px 0;
    text-transform: uppercase;
}

h2.cover-proj-title {
    font-size: 15pt;
    font-weight: bold;
    margin: 6px 0 16px 0;
    color: #0d3b66;
}

.cover-text {
    font-size: 11pt;
    margin: 4px 0;
    line-height: 1.35;
}

.degree-label {
    font-size: 14pt;
    font-weight: bold;
    margin: 12px 0 4px 0;
    text-transform: uppercase;
}

.batch-label {
    font-size: 11.5pt;
    font-weight: bold;
    margin-bottom: 20px;
}

.meta-split {
    margin: 18px 0;
    width: 100%;
}

.univ-emblem {
    width: 105px;
    height: 105px;
    border-radius: 50%;
    border: 3px solid #b22222;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: bold;
    color: #b22222;
    font-size: 10pt;
    text-transform: uppercase;
    background-color: #fffaf0;
    margin: 12px auto;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.univ-title {
    font-size: 13.5pt;
    font-weight: bold;
    text-transform: uppercase;
    margin: 3px 0;
    color: #b22222;
}

.dept-title {
    font-size: 12.5pt;
    font-weight: bold;
    margin: 3px 0;
}

.location-title {
    font-size: 11.5pt;
    margin: 2px 0 10px 0;
}

/* Headings */
h1.chap-header {
    text-align: center;
    font-size: 15pt;
    font-weight: bold;
    text-transform: uppercase;
    margin-top: 24px;
    margin-bottom: 4px;
    letter-spacing: 0.5px;
}

h1.chap-title {
    text-align: center;
    font-size: 17pt;
    font-weight: bold;
    text-transform: uppercase;
    margin-top: 0;
    margin-bottom: 20px;
    letter-spacing: 0.5px;
    border-bottom: 2px solid #0d3b66;
    padding-bottom: 5px;
}

h2.sec-title {
    font-size: 13pt;
    font-weight: bold;
    margin-top: 18px;
    margin-bottom: 8px;
    color: #0d3b66;
    border-bottom: 1px solid #ddd;
    padding-bottom: 3px;
}

h3.subsec-title {
    font-size: 11.5pt;
    font-weight: bold;
    margin-top: 14px;
    margin-bottom: 5px;
}

h4.subsubsec-title {
    font-size: 11pt;
    font-weight: bold;
    font-style: italic;
    margin-top: 10px;
    margin-bottom: 4px;
}

p {
    margin-top: 0;
    margin-bottom: 8px;
    text-indent: 24px;
}

p.no-indent {
    text-indent: 0;
}

ul, ol {
    margin-top: 3px;
    margin-bottom: 10px;
    padding-left: 30px;
}

li {
    margin-bottom: 4px;
}

/* Tables */
table.report-table {
    width: 100%;
    border-collapse: collapse;
    margin: 12px 0 16px 0;
    font-size: 9.5pt;
}

table.report-table th, table.report-table td {
    border: 1px solid #444;
    padding: 6px 8px;
    vertical-align: middle;
}

table.report-table th {
    background-color: #f0f4f8;
    font-weight: bold;
    text-align: center;
    color: #0d3b66;
}

.table-caption {
    text-align: center;
    font-weight: bold;
    font-size: 10.5pt;
    margin: 10px 0 5px 0;
    color: #222;
}

/* Images & Captions */
.img-container {
    text-align: center;
    margin: 14px 0 6px 0;
    page-break-inside: avoid;
}

.figure-caption {
    text-align: center;
    font-weight: bold;
    font-size: 10.5pt;
    margin: 6px 0 12px 0;
    color: #222;
}

.img-explanation {
    background-color: #fcfcfc;
    border-left: 3.5px solid #0d3b66;
    padding: 8px 12px;
    margin: 6px 0 14px 0;
    font-size: 10pt;
    line-height: 1.4;
}

.signature-table {
    width: 100%;
    margin-top: 40px;
    border-collapse: collapse;
}

.signature-table td {
    vertical-align: top;
    padding: 8px;
}

.sig-line {
    border-top: 1px solid #000;
    margin-top: 45px;
    padding-top: 4px;
    text-align: center;
    font-size: 10pt;
}

pre.code-block {
    background-color: #f6f8fa;
    border: 1px solid #d0d7de;
    border-radius: 4px;
    padding: 8px 12px;
    font-family: 'Consolas', 'Courier New', monospace;
    font-size: 8.8pt;
    line-height: 1.32;
    overflow-x: auto;
    margin: 8px 0 12px 0;
    white-space: pre-wrap;
    text-indent: 0;
}
</style>
</head>
<body>
""")

html_parts.append(sections_prelim.get_prelim_html())
html_parts.append(sections_ch1_ch2.get_ch1_ch2_html(img_usecase, img_storage, img_sequence))
html_parts.append(sections_ch3_ch4.get_ch3_ch4_html(img_client_arch, img_topology, img_er, img_cicd))
html_parts.append(sections_ch5_ch8.get_ch5_ch8_html())
html_parts.append("</body></html>")

full_html = "".join(html_parts)

with open(HTML_OUT, "w", encoding="utf-8") as f:
    f.write(full_html)
print(f"HTML generated successfully: {HTML_OUT} ({len(full_html)} bytes)")

print("3. Compiling PDF via Chrome Headless...")
chrome_path = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
if not os.path.exists(chrome_path):
    chrome_path = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"

file_url = "file:///" + HTML_OUT.replace("\\", "/")
cmd = [
    chrome_path,
    "--headless",
    "--disable-gpu",
    "--no-sandbox",
    "--run-all-compositor-stages-before-draw",
    f"--print-to-pdf={PDF_OUT}",
    file_url
]

res = subprocess.run(cmd, capture_output=True, text=True)
print("Chrome execution finished with exit code:", res.returncode)

if os.path.exists(PDF_OUT):
    size = os.path.getsize(PDF_OUT)
    with open(PDF_OUT, "rb") as f:
        pdf_bytes = f.read()
    page_matches = re.findall(rb'/Type\s*/Page\b', pdf_bytes)
    print(f"SUCCESS: PDF created at {PDF_OUT}")
    print(f"Size: {size:,} bytes | Page count: {len(page_matches)} pages")
else:
    print("ERROR: PDF was not generated!")

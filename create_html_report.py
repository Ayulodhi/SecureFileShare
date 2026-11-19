# -*- coding: utf-8 -*-
"""
create_html_report.py
Generates the complete, high-fidelity academic HTML report for SecureFileShare
and compiles it to PDF using Google Chrome Headless.
"""

import os
import sys
import base64
import subprocess
import re

BASE_DIR = r"E:\project emp\pbl java"
IMG_DIR = os.path.join(BASE_DIR, "images")
HTML_FILE = os.path.join(BASE_DIR, "PROJECT_REPORT.html")
PDF_FILE = os.path.join(BASE_DIR, "PROJECT_REPORT.pdf")

def b64(filename):
    p = os.path.join(IMG_DIR, filename)
    if not os.path.exists(p):
        return ""
    ext = os.path.splitext(filename)[1].lower().replace(".", "")
    if ext == "jpg": ext = "jpeg"
    with open(p, "rb") as f:
        return f"data:image/{ext};base64," + base64.b64encode(f.read()).decode("utf-8")

img_usecase = b64("fig1_use_case_overview.png")
img_storage = b64("fig2_storage_workflow_activity.png")
img_sequence = b64("fig3_sequence_upload_pipeline.png")
img_client_arch = b64("fig4_client_service_arch.png")
img_topology = b64("fig5_component_storage_topology.jpg")
img_er = b64("fig6_database_er_diagram.png")
img_cicd = b64("fig7_cicd_release_pipeline.png")

html = []

html.append("""<!DOCTYPE html>
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

print("Appending preliminary pages...")

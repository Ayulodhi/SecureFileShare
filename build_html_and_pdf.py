# -*- coding: utf-8 -*-
import os
import subprocess
import base64
import re

BASE_DIR = r"E:\project emp\pbl java"
HTML_OUT = os.path.join(BASE_DIR, "PROJECT_REPORT.html")
PDF_OUT = os.path.join(BASE_DIR, "PROJECT_REPORT.pdf")

def get_img_tag(filename, alt_text, width_percent=85):
    img_path = os.path.join(BASE_DIR, "images", filename)
    if os.path.exists(img_path):
        ext = os.path.splitext(filename)[1].lower().replace(".", "")
        if ext == "jpg": ext = "jpeg"
        with open(img_path, "rb") as f:
            b64 = base64.b64encode(f.read()).decode("utf-8")
        return f'<div class="img-container"><img src="data:image/{ext};base64,{b64}" alt="{alt_text}" style="max-width: {width_percent}%; height: auto; display: block; margin: 0 auto; border: 1px solid #ddd; border-radius: 4px; box-shadow: 0 2px 4px rgba(0,0,0,0.05);" /></div>'
    return f'<p style="color:red; text-align:center;">[Image {filename} not found]</p>'

css_styles = """
<style>
@page {
    size: A4;
    margin: 22mm 18mm 22mm 22mm;
    @bottom-center {
        content: counter(page);
        font-family: 'Times New Roman', Times, serif;
        font-size: 10pt;
    }
}

body {
    font-family: 'Times New Roman', Times, serif;
    font-size: 12pt;
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

.title-page {
    text-align: center;
    padding-top: 20px;
    height: 90vh;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
}

.double-line {
    border-top: 3px double #000;
    border-bottom: 1px solid #000;
    height: 4px;
    margin: 15px 0;
}

h1.report-title {
    font-size: 20pt;
    font-weight: bold;
    letter-spacing: 1px;
    margin: 15px 0 10px 0;
    text-transform: uppercase;
}

h2.project-name {
    font-size: 16pt;
    font-weight: bold;
    margin: 10px 0 20px 0;
    color: #0d3b66;
}

.title-subtext {
    font-size: 12pt;
    margin: 6px 0;
}

.degree-name {
    font-size: 15pt;
    font-weight: bold;
    margin: 15px 0 5px 0;
    text-transform: uppercase;
}

.batch-name {
    font-size: 12pt;
    font-weight: bold;
    margin-bottom: 25px;
}

.submission-meta {
    margin: 25px 0;
    display: flex;
    justify-content: space-between;
    text-align: left;
}

.meta-col {
    width: 48%;
}

.univ-emblem {
    width: 110px;
    height: auto;
    margin: 15px auto;
    display: block;
}

.univ-name {
    font-size: 14pt;
    font-weight: bold;
    text-transform: uppercase;
    margin: 4px 0;
}

.dept-name {
    font-size: 13pt;
    font-weight: bold;
    margin: 4px 0;
}

.univ-location {
    font-size: 12pt;
    margin: 2px 0 15px 0;
}

/* Headings */
h1.chap-num {
    text-align: center;
    font-size: 16pt;
    font-weight: bold;
    text-transform: uppercase;
    margin-top: 30px;
    margin-bottom: 5px;
    letter-spacing: 0.5px;
}

h1.chap-title {
    text-align: center;
    font-size: 18pt;
    font-weight: bold;
    text-transform: uppercase;
    margin-top: 0;
    margin-bottom: 25px;
    letter-spacing: 0.5px;
    border-bottom: 2px solid #0d3b66;
    padding-bottom: 8px;
}

h2.sec-heading {
    font-size: 14pt;
    font-weight: bold;
    margin-top: 22px;
    margin-bottom: 8px;
    color: #0d3b66;
    border-bottom: 1px solid #e0e0e0;
    padding-bottom: 4px;
}

h3.subsec-heading {
    font-size: 12.5pt;
    font-weight: bold;
    margin-top: 16px;
    margin-bottom: 6px;
}

h4.subsubsec-heading {
    font-size: 12pt;
    font-weight: bold;
    font-style: italic;
    margin-top: 12px;
    margin-bottom: 4px;
}

p {
    margin-top: 0;
    margin-bottom: 10px;
    text-indent: 25px;
}

p.no-indent {
    text-indent: 0;
}

ul, ol {
    margin-top: 4px;
    margin-bottom: 12px;
    padding-left: 35px;
}

li {
    margin-bottom: 5px;
}

/* Tables */
table.report-table {
    width: 100%;
    border-collapse: collapse;
    margin: 16px 0 20px 0;
    font-size: 10.5pt;
}

table.report-table th, table.report-table td {
    border: 1px solid #333;
    padding: 7px 9px;
    vertical-align: middle;
}

table.report-table th {
    background-color: #f0f4f8;
    font-weight: bold;
    text-align: center;
    color: #0d3b66;
}

table.toc-table {
    width: 100%;
    border-collapse: collapse;
    margin: 15px 0;
    font-size: 11pt;
}

table.toc-table td {
    border: 1px solid #555;
    padding: 6px 10px;
}

table.toc-table th {
    border: 1px solid #333;
    padding: 8px 10px;
    background-color: #f0f4f8;
    color: #0d3b66;
}

/* Images & Captions */
.img-container {
    text-align: center;
    margin: 18px 0 8px 0;
    page-break-inside: avoid;
}

.figure-caption {
    text-align: center;
    font-weight: bold;
    font-size: 11pt;
    margin: 8px 0 16px 0;
    color: #222;
}

.table-caption {
    text-align: center;
    font-weight: bold;
    font-size: 11pt;
    margin: 14px 0 6px 0;
    color: #222;
}

/* Callouts / Highlights */
.callout-box {
    border-left: 4px solid #0d3b66;
    background-color: #f8fafc;
    padding: 10px 14px;
    margin: 14px 0;
    font-size: 11pt;
}

.signature-block {
    margin-top: 40px;
    display: flex;
    justify-content: space-between;
}

.signature-line {
    border-top: 1px solid #333;
    width: 220px;
    margin-top: 45px;
    text-align: center;
    font-size: 11pt;
    padding-top: 4px;
}

/* Code Blocks */
pre.code-block {
    background-color: #f4f6f8;
    border: 1px solid #d0d7de;
    border-radius: 4px;
    padding: 10px 14px;
    font-family: 'Consolas', 'Courier New', monospace;
    font-size: 9.5pt;
    line-height: 1.35;
    overflow-x: auto;
    margin: 12px 0 16px 0;
    white-space: pre-wrap;
    text-indent: 0;
}
</style>
"""

print("Writing HTML sections...")
# -*- coding: utf-8 -*-
import os
import subprocess
import base64
import re

BASE_DIR = r"E:\project emp\pbl java"
HTML_OUT = os.path.join(BASE_DIR, "PROJECT_REPORT.html")
PDF_OUT = os.path.join(BASE_DIR, "PROJECT_REPORT.pdf")

def get_img_tag(filename, alt_text, max_w="82%"):
    img_path = os.path.join(BASE_DIR, "images", filename)
    if os.path.exists(img_path):
        ext = os.path.splitext(filename)[1].lower().replace(".", "")
        if ext == "jpg": ext = "jpeg"
        with open(img_path, "rb") as f:
            b64 = base64.b64encode(f.read()).decode("utf-8")
        return f'<div class="img-container"><img src="data:image/{ext};base64,{b64}" alt="{alt_text}" style="max-width: {max_w}; max-height: 480px; height: auto; display: block; margin: 0 auto; border: 1px solid #ccc; border-radius: 4px; box-shadow: 0 2px 5px rgba(0,0,0,0.08);" /></div>'
    return f'<p style="color:red; text-align:center;">[Image {filename} not found]</p>'

print("Assembling complete academic report HTML...")

html_head = """<!DOCTYPE html>
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
    line-height: 1.42;
    color: #111;
    margin: 0;
    padding: 0;
    text-align: justify;
}

.page-break {
    page-break-after: always;
    break-after: page;
}

.cover-page {
    text-align: center;
    padding-top: 15px;
    height: 92vh;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
}

.double-line {
    border-top: 3px double #000;
    border-bottom: 1px solid #000;
    height: 4px;
    margin: 12px 0 16px 0;
}

h1.cover-title {
    font-size: 18pt;
    font-weight: bold;
    letter-spacing: 1px;
    margin: 10px 0 6px 0;
    text-transform: uppercase;
}

h2.project-title {
    font-size: 15.5pt;
    font-weight: bold;
    margin: 8px 0 16px 0;
    color: #0d3b66;
}

.cover-subtext {
    font-size: 11pt;
    margin: 4px 0;
    line-height: 1.35;
}

.degree-title {
    font-size: 14pt;
    font-weight: bold;
    margin: 14px 0 4px 0;
    text-transform: uppercase;
}

.batch-title {
    font-size: 11pt;
    font-weight: bold;
    margin-bottom: 20px;
}

.submission-meta {
    margin: 20px 0;
    width: 100%;
}

.univ-emblem-box {
    margin: 15px auto;
    width: 110px;
    height: 110px;
    border-radius: 50%;
    border: 3px solid #b22222;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: bold;
    color: #b22222;
    font-size: 11pt;
    text-transform: uppercase;
    background-color: #fffaf0;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.univ-name {
    font-size: 13.5pt;
    font-weight: bold;
    text-transform: uppercase;
    margin: 3px 0;
    color: #b22222;
}

.dept-name {
    font-size: 12.5pt;
    font-weight: bold;
    margin: 3px 0;
}

.univ-location {
    font-size: 11.5pt;
    margin: 2px 0 10px 0;
}

/* Headings */
h1.chap-header {
    text-align: center;
    font-size: 15pt;
    font-weight: bold;
    text-transform: uppercase;
    margin-top: 25px;
    margin-bottom: 4px;
    letter-spacing: 0.5px;
}

h1.chap-title {
    text-align: center;
    font-size: 17pt;
    font-weight: bold;
    text-transform: uppercase;
    margin-top: 0;
    margin-bottom: 22px;
    letter-spacing: 0.5px;
    border-bottom: 2px solid #0d3b66;
    padding-bottom: 6px;
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
    padding-left: 32px;
}

li {
    margin-bottom: 4px;
}

/* Tables */
table.report-table {
    width: 100%;
    border-collapse: collapse;
    margin: 12px 0 16px 0;
    font-size: 10pt;
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
    margin: 6px 0 14px 0;
    color: #222;
}

.img-explanation {
    background-color: #fdfdfd;
    border-left: 3px solid #0d3b66;
    padding: 8px 12px;
    margin: 8px 0 14px 0;
    font-size: 10.5pt;
    line-height: 1.38;
}

.signature-table {
    width: 100%;
    margin-top: 40px;
    border-collapse: collapse;
}

.signature-table td {
    vertical-align: top;
    padding: 10px;
}

.sig-line {
    border-top: 1px solid #000;
    margin-top: 45px;
    padding-top: 4px;
    text-align: center;
    font-size: 10.5pt;
}

pre.code-block {
    background-color: #f6f8fa;
    border: 1px solid #d0d7de;
    border-radius: 4px;
    padding: 8px 12px;
    font-family: 'Consolas', 'Courier New', monospace;
    font-size: 9pt;
    line-height: 1.32;
    overflow-x: auto;
    margin: 8px 0 12px 0;
    white-space: pre-wrap;
    text-indent: 0;
}
</style>
</head>
<body>
"""

print("Writing chapters content...")

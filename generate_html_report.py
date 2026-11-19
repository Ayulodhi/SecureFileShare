# -*- coding: utf-8 -*-
"""
generate_html_report.py
Generates the comprehensive academic HTML report for SecureFileShare and compiles it to PDF via Chrome headless.
"""

import os
import sys
import subprocess
import re

BASE_DIR = r"E:\project emp\pbl java"
HTML_OUT = os.path.join(BASE_DIR, "PROJECT_REPORT.html")
PDF_OUT = os.path.join(BASE_DIR, "PROJECT_REPORT.pdf")

# Read images as base64 or relative file paths so Chrome prints them flawlessly
import base64

def get_img_tag(filename, alt_text):
    img_path = os.path.join(BASE_DIR, "images", filename)
    if os.path.exists(img_path):
        ext = os.path.splitext(filename)[1].lower().replace(".", "")
        if ext == "jpg": ext = "jpeg"
        with open(img_path, "rb") as f:
            b64 = base64.b64encode(f.read()).decode("utf-8")
        return f'<div class="img-container"><img src="data:image/{ext};base64,{b64}" alt="{alt_text}" class="report-img" /></div>'
    return f'<p style="color:red; text-align:center;">[Image {filename} not found]</p>'

print("Writing comprehensive academic HTML report...")

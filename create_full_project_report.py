# -*- coding: utf-8 -*-
"""
create_full_project_report.py
Generates the comprehensive academic report for SecureFileShare:
- PROJECT_REPORT.html
- PROJECT_REPORT.pdf (via Chrome headless)
- PROJECT_REPORT.docx (via python-docx)
- PROJECT_REPORT.md
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
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

BASE_DIR = r"E:\project emp\pbl java"
IMG_DIR = os.path.join(BASE_DIR, "images")

def get_base64_image(filename):
    p = os.path.join(IMG_DIR, filename)
    if not os.path.exists(p):
        return ""
    ext = os.path.splitext(filename)[1].lower().replace(".", "")
    if ext == "jpg": ext = "jpeg"
    with open(p, "rb") as f:
        return f"data:image/{ext};base64," + base64.b64encode(f.read()).decode("utf-8")

print("Building full report generator...")

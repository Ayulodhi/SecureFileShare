# -*- coding: utf-8 -*-
"""
build_complete_report.py
Generates:
1. PROJECT_REPORT.html
2. PROJECT_REPORT.pdf (via Chrome headless)
3. PROJECT_REPORT.docx (via python-docx)
4. PROJECT_REPORT.md
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
HTML_FILE = os.path.join(BASE_DIR, "PROJECT_REPORT.html")
PDF_FILE = os.path.join(BASE_DIR, "PROJECT_REPORT.pdf")
DOCX_FILE = os.path.join(BASE_DIR, "PROJECT_REPORT.docx")
MD_FILE = os.path.join(BASE_DIR, "PROJECT_REPORT.md")

print("Starting complete report compilation...")

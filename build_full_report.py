# -*- coding: utf-8 -*-
"""
build_full_report.py
Generates the complete MCA Project Report for SecureFileShare:
1. PROJECT_REPORT.html (Styled academic document with CSS print styling)
2. PROJECT_REPORT.pdf (Compiled via Google Chrome headless, target 35-40 pages)
3. PROJECT_REPORT.docx (Editable Microsoft Word document formatted for Graphic Era Deemed to be University)
4. PROJECT_REPORT.md (Complete Markdown report)
"""

import os
import sys
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
IMAGES_DIR = os.path.join(BASE_DIR, "images")

print("Initializing SecureFileShare Report Generation...")

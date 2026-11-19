# -*- coding: utf-8 -*-
"""
generate_all.py
Builds:
1. E:\project emp\pbl java\PROJECT_REPORT.html
2. E:\project emp\pbl java\PROJECT_REPORT.pdf (via Chrome headless)
3. E:\project emp\pbl java\PROJECT_REPORT.docx (via python-docx)
4. E:\project emp\pbl java\PROJECT_REPORT.md
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
HTML_OUT = os.path.join(BASE_DIR, "PROJECT_REPORT.html")
PDF_OUT = os.path.join(BASE_DIR, "PROJECT_REPORT.pdf")
DOCX_OUT = os.path.join(BASE_DIR, "PROJECT_REPORT.docx")
MD_OUT = os.path.join(BASE_DIR, "PROJECT_REPORT.md")

def b64_img(filename):
    p = os.path.join(IMG_DIR, filename)
    if not os.path.exists(p):
        return ""
    ext = os.path.splitext(filename)[1].lower().replace(".", "")
    if ext == "jpg": ext = "jpeg"
    with open(p, "rb") as f:
        return f"data:image/{ext};base64," + base64.b64encode(f.read()).decode("utf-8")

img_usecase_b64 = b64_img("fig1_use_case_overview.png")
img_storage_b64 = b64_img("fig2_storage_workflow_activity.png")
img_sequence_b64 = b64_img("fig3_sequence_upload_pipeline.png")
img_client_arch_b64 = b64_img("fig4_client_service_arch.png")
img_topology_b64 = b64_img("fig5_component_storage_topology.jpg")
img_er_b64 = b64_img("fig6_database_er_diagram.png")
img_cicd_b64 = b64_img("fig7_cicd_release_pipeline.png")

print("Building HTML report content...")

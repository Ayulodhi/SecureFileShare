# -*- coding: utf-8 -*-
"""
generate_docx_full.py
Constructs the complete academic DOCX report for SecureFileShare.
"""

import os
import sys
import docx
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

BASE_DIR = r"E:\project emp\pbl java"
IMG_DIR = os.path.join(BASE_DIR, "images")
DOCX_OUT = os.path.join(BASE_DIR, "PROJECT_REPORT.docx")

def set_cell_shading(cell, color_hex):
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{color_hex}"/>')
    cell._tc.get_or_add_tcPr().append(shd)

def set_cell_padding(cell, top=100, bottom=100, left=130, right=130):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = OxmlElement('w:tcMar')
    for m, val in [('top', top), ('bottom', bottom), ('left', left), ('right', right)]:
        node = OxmlElement(f'w:{m}')
        node.set(qn('w:w'), str(val))
        node.set(qn('w:type'), 'dxa')
        tcMar.append(node)
    tcPr.append(tcMar)

def set_table_borders(table):
    tblPr = table._tbl.tblPr
    borders = parse_xml(
        f'<w:tblBorders {nsdecls("w")}>'
        f'<w:top w:val="single" w:sz="6" w:space="0" w:color="555555"/>'
        f'<w:bottom w:val="single" w:sz="6" w:space="0" w:color="555555"/>'
        f'<w:insideH w:val="single" w:sz="4" w:space="0" w:color="CCCCCC"/>'
        f'<w:insideV w:val="single" w:sz="4" w:space="0" w:color="CCCCCC"/>'
        f'<w:left w:val="single" w:sz="6" w:space="0" w:color="555555"/>'
        f'<w:right w:val="single" w:sz="6" w:space="0" w:color="555555"/>'
        f'</w:tblBorders>'
    )
    tblPr.append(borders)

doc = Document()
for s in doc.sections:
    s.page_width = Inches(8.27)
    s.page_height = Inches(11.69)
    s.top_margin = Inches(0.85)
    s.bottom_margin = Inches(0.85)
    s.left_margin = Inches(0.95)
    s.right_margin = Inches(0.85)

normal = doc.styles['Normal']
normal.font.name = 'Times New Roman'
normal.font.size = Pt(11.5)
normal.font.color.rgb = RGBColor(0x11, 0x11, 0x11)
normal.paragraph_format.line_spacing = 1.2
normal.paragraph_format.space_after = Pt(4)

def p(text="", bold_prefix="", indent=True, align=WD_ALIGN_PARAGRAPH.JUSTIFY):
    para = doc.add_paragraph()
    para.alignment = align
    if indent and align == WD_ALIGN_PARAGRAPH.JUSTIFY:
        para.paragraph_format.first_line_indent = Inches(0.3)
    if bold_prefix:
        r_pre = para.add_run(bold_prefix)
        r_pre.font.name = 'Times New Roman'
        r_pre.font.size = Pt(11.5)
        r_pre.font.bold = True
    r = para.add_run(text)
    r.font.name = 'Times New Roman'
    r.font.size = Pt(11.5)
    return para

def chap(num, title):
    doc.add_page_break()
    p1 = doc.add_paragraph()
    p1.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p1.paragraph_format.space_before = Pt(16)
    p1.paragraph_format.space_after = Pt(2)
    r1 = p1.add_run(f"Chapter {num}")
    r1.font.name = 'Times New Roman'
    r1.font.size = Pt(15)
    r1.font.bold = True
    r1.font.color.rgb = RGBColor(0x0D, 0x3B, 0x66)

    p2 = doc.add_paragraph()
    p2.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p2.paragraph_format.space_before = Pt(0)
    p2.paragraph_format.space_after = Pt(18)
    r2 = p2.add_run(title.upper())
    r2.font.name = 'Times New Roman'
    r2.font.size = Pt(17)
    r2.font.bold = True
    r2.font.color.rgb = RGBColor(0x0D, 0x3B, 0x66)

def sec(title):
    para = doc.add_paragraph()
    para.paragraph_format.space_before = Pt(12)
    para.paragraph_format.space_after = Pt(3)
    r = para.add_run(title)
    r.font.name = 'Times New Roman'
    r.font.size = Pt(13)
    r.font.bold = True
    r.font.color.rgb = RGBColor(0x0D, 0x3B, 0x66)

def subsec(title):
    para = doc.add_paragraph()
    para.paragraph_format.space_before = Pt(9)
    para.paragraph_format.space_after = Pt(2)
    r = para.add_run(title)
    r.font.name = 'Times New Roman'
    r.font.size = Pt(11.5)
    r.font.bold = True

def bullet(text, bold_prefix=""):
    para = doc.add_paragraph(style='List Bullet')
    para.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    para.paragraph_format.space_after = Pt(2.5)
    if bold_prefix:
        r_pre = para.add_run(bold_prefix)
        r_pre.font.name = 'Times New Roman'
        r_pre.font.size = Pt(11)
        r_pre.font.bold = True
    r = para.add_run(text)
    r.font.name = 'Times New Roman'
    r.font.size = Pt(11)

def tbl(caption, headers, rows):
    p_cap = doc.add_paragraph()
    p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_cap.paragraph_format.space_before = Pt(8)
    p_cap.paragraph_format.space_after = Pt(3)
    r_cap = p_cap.add_run(caption)
    r_cap.font.name = 'Times New Roman'
    r_cap.font.size = Pt(10)
    r_cap.font.bold = True

    t = doc.add_table(rows=len(rows) + 1, cols=len(headers))
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(t)

    for i, h in enumerate(headers):
        cell = t.rows[0].cells[i]
        cell.text = h
        set_cell_shading(cell, "F0F4F8")
        set_cell_padding(cell, top=80, bottom=80, left=100, right=100)
        p_cell = cell.paragraphs[0]
        p_cell.alignment = WD_ALIGN_PARAGRAPH.CENTER
        for run in p_cell.runs:
            run.font.name = 'Times New Roman'
            run.font.size = Pt(9.5)
            run.font.bold = True
            run.font.color.rgb = RGBColor(0x0D, 0x3B, 0x66)

    for r_idx, r in enumerate(rows):
        for c_idx, val in enumerate(r):
            cell = t.rows[r_idx + 1].cells[c_idx]
            cell.text = str(val)
            set_cell_padding(cell, top=60, bottom=60, left=90, right=90)
            p_cell = cell.paragraphs[0]
            if c_idx == 0 or len(str(val)) < 8:
                p_cell.alignment = WD_ALIGN_PARAGRAPH.CENTER
            else:
                p_cell.alignment = WD_ALIGN_PARAGRAPH.LEFT
            for run in p_cell.runs:
                run.font.name = 'Times New Roman'
                run.font.size = Pt(9)

    doc.add_paragraph().paragraph_format.space_after = Pt(4)

def fig(filename, caption, explanation, width=5.2):
    p_path = os.path.join(IMG_DIR, filename)
    if os.path.exists(p_path):
        p_im = doc.add_paragraph()
        p_im.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_im.paragraph_format.space_before = Pt(10)
        p_im.paragraph_format.space_after = Pt(2)
        p_im.add_run().add_picture(p_path, width=Inches(width))

        p_cp = doc.add_paragraph()
        p_cp.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_cp.paragraph_format.space_before = Pt(2)
        p_cp.paragraph_format.space_after = Pt(6)
        r_cp = p_cp.add_run(caption)
        r_cp.font.name = 'Times New Roman'
        r_cp.font.size = Pt(10)
        r_cp.font.bold = True

        tbl_box = doc.add_table(rows=1, cols=1)
        tbl_box.alignment = WD_TABLE_ALIGNMENT.CENTER
        c = tbl_box.rows[0].cells[0]
        c.width = Inches(6.3)
        set_cell_shading(c, "FCFCFC")
        set_cell_padding(c, top=80, bottom=80, left=120, right=120)
        p_box = c.paragraphs[0]
        p_box.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        r_t = p_box.add_run(f"Detailed Technical Explanation of {caption.split(':')[0]}:\n")
        r_t.font.name = 'Times New Roman'
        r_t.font.size = Pt(9.5)
        r_t.font.bold = True
        r_t.font.color.rgb = RGBColor(0x0D, 0x3B, 0x66)
        r_b = p_box.add_run(explanation)
        r_b.font.name = 'Times New Roman'
        r_b.font.size = Pt(9)

        doc.add_paragraph().paragraph_format.space_after = Pt(6)

print("Writing Title and Prelims...")

# TITLE PAGE
p("PROJECT REPORT ON", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False)
p("SecureFileShare – Zero-Knowledge Encrypted File Sharing & Vault Platform", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False)
p("Submitted to Department of Computer Applications in partial fulfillment for the award of the degree of", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False)
p("MASTER OF COMPUTER APPLICATIONS", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False)
p("Batch (2025–2027)\n", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False)

p("Submitted by: Ayush Lodhi (GE - 25113399)", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False)
p("Under the Guidance of: Project Supervisor / Faculty Guide", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False)
p("(Professor / Associate Professor)\n\n", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False)

p("Department of Computer Applications", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False)
p("Graphic Era (Deemed to be University)", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False)
p("Dehradun, Uttarakhand", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False)
p("November-2026", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False)

# DECLARATION
doc.add_page_break()
p("CANDIDATE’S DECLARATION", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False)
p("I hereby certify that the work presented in this project report entitled 'SecureFileShare – Zero-Knowledge Encrypted File Sharing & Vault Platform' in partial fulfilment of the requirements for the award of the degree of Master of Computer Applications is a bonafide work carried out by me during the period of August 2026 to November 2026 under the supervision of Faculty Guide, Department of Computer Applications, Graphic Era Deemed to be University, Dehradun, India.")
p("This work has not been submitted elsewhere for the award of a degree/diploma/certificate.")
p("\n\nAyush Lodhi\nName and Signature of Candidate", align=WD_ALIGN_PARAGRAPH.RIGHT, indent=False)
p("This is to certify that the above mentioned statement in the candidate’s declaration is correct to the best of my knowledge.", indent=False)
p("Date: ____________________\t\tName and Signature of Guide", indent=False)
p("\nSignature of Supervisor\t\tSignature of External Examiner\n\nHOD, Department of Computer Applications", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False)

# CERTIFICATE OF ORIGINALITY
doc.add_page_break()
p("CERTIFICATE OF ORIGINALITY", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False)
p("This is to certify that the project report entitled 'SecureFileShare – Zero-Knowledge Encrypted File Sharing & Vault Platform' submitted to Graphic Era University, Dehradun in partial fulfilment of the requirement for the award of the degree of MASTER OF COMPUTER APPLICATIONS (MCA), is an authentic and original work carried out by Mr. Ayush Lodhi with enrolment number GE-25113399 under my supervision and guidance.")
p("The matter embodied in this project is genuine work done by the student and has not been submitted whether to this University or to any other University / Institute for the fulfilment of the requirements of any course of study.")
p("\n\nSignature of the Student:\t\t\tSignature of the Guide:", indent=False)
p("Date:\t\t\t\t\tDate:", indent=False)
p("Enrolment No.: GE-25113399\nName and Address of Student: Ayush Lodhi, Department of Computer Applications, Graphic Era University, Dehradun", indent=False)

# ACKNOWLEDGEMENT
doc.add_page_break()
p("ACKNOWLEDGEMENT", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False)
p("First of all, I would like to express my sincere and wholehearted gratitude to my project guide, Faculty Guide, Department of Computer Applications, Graphic Era (Deemed to be University), for invaluable guidance, continuous support, and encouragement throughout the development of this project. Her insightful suggestions and constant motivation helped me to successfully complete this work.")
p("I would also like to thank all the faculty members of the Department of Computer Applications for providing me with the necessary knowledge, resources, and a supportive academic environment that greatly contributed to the successful completion of this project.")
p("I am grateful to my friends and colleagues for their cooperation, constructive suggestions, and continuous support during the entire duration of the project. Their help and encouragement played an important role in overcoming various technical challenges faced during the development process.")
p("Last but not least, I thank everyone who directly or indirectly contributed to the successful completion of this project.")
p("\n\nAyush Lodhi", align=WD_ALIGN_PARAGRAPH.RIGHT, indent=False)

# LIST OF ABBREVIATIONS
doc.add_page_break()
p("LIST OF ABBREVIATIONS", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False)
tbl("List of Abbreviations", ["S. No.", "Abbr", "Full Form", "Chapter"], [
    ["1", "AES", "Advanced Encryption Standard", "Ch. 1"],
    ["2", "GCM", "Galois/Counter Mode", "Ch. 1"],
    ["3", "RSA", "Rivest–Shamir–Adleman Asymmetric Cryptosystem", "Ch. 1"],
    ["4", "OAEP", "Optimal Asymmetric Encryption Padding", "Ch. 1"],
    ["5", "ECDSA", "Elliptic Curve Digital Signature Algorithm", "Ch. 1"],
    ["6", "SHA", "Secure Hash Algorithm", "Ch. 1"],
    ["7", "NIST", "National Institute of Standards and Technology", "Ch. 1"],
    ["8", "JWT", "JSON Web Token", "Ch. 1"],
    ["9", "API", "Application Programming Interface", "Ch. 1"],
    ["10", "REST", "Representational State Transfer", "Ch. 1"],
    ["11", "DFD", "Data Flow Diagram", "Ch. 2"],
    ["12", "SRS", "Software Requirements Specification", "Ch. 2"],
    ["13", "ER", "Entity Relationship", "Ch. 3"],
    ["14", "WBS", "Work Breakdown Structure", "Ch. 4"],
    ["15", "PERT", "Program Evaluation and Review Technique", "Ch. 4"],
    ["16", "CI/CD", "Continuous Integration / Continuous Deployment", "Ch. 4"],
    ["17", "GHCR", "GitHub Container Registry", "Ch. 4"],
    ["18", "CSP", "Content Security Policy", "Ch. 7"],
    ["19", "CORS", "Cross-Origin Resource Sharing", "Ch. 7"],
    ["20", "RBAC", "Role-Based Access Control", "Ch. 7"]
])

# LIST OF FIGURES
doc.add_page_break()
p("LIST OF FIGURES", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False)
tbl("List of Figures", ["S. No.", "Figure No.", "Title", "Page No."], [
    ["1", "Figure 2.1", "Use Case Diagram of SecureFileShare Platform", "22"],
    ["2", "Figure 2.2", "Sequence Diagram of Recipient Ciphertext Upload & Key Sharing Pipeline", "26"],
    ["3", "Figure 2.3", "Activity Diagram: Encrypted File Storage & Retrieval Flow", "28"],
    ["4", "Figure 3.1", "Client-Service Monorepo Architecture of SecureFileShare", "32"],
    ["5", "Figure 3.2", "Detailed Microservice Component & Storage Topology", "34"],
    ["6", "Figure 3.3", "Database Entity Relationship (ER) Diagram", "37"],
    ["7", "Figure 4.1", "Continuous Integration and Continuous Deployment (CI/CD) Pipeline", "70"]
])

# LIST OF TABLES
doc.add_page_break()
p("LIST OF TABLES", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False)
tbl("List of Tables", ["S. No.", "Table No.", "Title", "Page No."], [
    ["1", "Table 2.1", "Functional Requirements of SecureFileShare", "20"],
    ["2", "Table 2.2", "Non-Functional Requirements of SecureFileShare", "21"],
    ["3", "Table 3.1", "Communication Flow Between System Layers", "33"],
    ["4", "Table 3.2", "Entities and Attributes of SecureFileShare System", "36"],
    ["5", "Table 3.3", "Database Tables and Structure Specifications", "38"],
    ["6", "Table 3.4", "User Interface Components and Description", "46"],
    ["7", "Table 4.1", "Work Breakdown Structure (WBS)", "58"],
    ["8", "Table 4.2", "Project Timeline (8-Week Lifecycle)", "59"],
    ["9", "Table 4.3", "Project Milestones & Target Deliverables", "61"],
    ["10", "Table 4.5", "Risk Analysis & Impact Matrix", "64"],
    ["11", "Table 4.6", "Cost Estimation of the Project", "67"],
    ["12", "Table 6.1", "Sample Encrypted Vault File Records", "80"],
    ["13", "Table 6.2", "Sample Audit Trail & Hash-Chain Verification Log", "82"],
    ["14", "Table 7.1", "Unit Testing Details & Validation Results", "84"],
    ["15", "Table 7.2", "System Test Cases & Security Validation Results", "87"],
    ["16", "Table 7.3", "User Roles and Access Permissions Matrix", "89"]
])

print("Writing Chapter 1...")
chap(1, "Introduction")
sec("1.1 Introduction")
p("File storage and transmission are foundational services of modern computing infrastructure across academic institutions, commercial enterprises, healthcare systems, and governmental bodies. Every day, terabytes of confidential electronic documents—including academic research records, examination question banks, financial audits, proprietary intellectual property, medical evaluations, and legal contracts—are transferred across public networks and stored in cloud repositories.")
p("However, mainstream public cloud storage providers (such as Google Drive, Microsoft OneDrive, and Dropbox) operate almost exclusively on a 'trusted-server' architectural model. In this traditional model, users upload files in plaintext or over transport-layer encrypted channels (TLS/HTTPS). Once received by the cloud service provider, incoming payloads are decrypted on application servers before being written to persistent block or object storage. Even when providers claim 'encryption at rest,' the cryptographic keys are managed, retained, and accessible by the provider’s internal infrastructure.")
p("This centralized custody model creates critical vulnerabilities including insider threats, subpoena risks, and server-side memory inspection vulnerabilities. To overcome these security flaws, SecureFileShare introduces an enterprise-grade, Zero-Knowledge End-to-End Encrypted File Sharing & Vault Platform.")

sec("1.2 Motivation")
p("The motivation behind developing SecureFileShare stems from pressing real-world data privacy mandates (such as GDPR, HIPAA, and DPDP) and the academic pursuit of applying modern distributed systems and advanced cryptography to solve cloud security vulnerabilities.")
p("Developing SecureFileShare provided the opportunity to address these pressing challenges by building an end-to-end distributed system utilizing Java 21, Spring Boot 3.3.4, PostgreSQL 16 with Flyway migrations, MinIO S3 distributed object storage, client-side AES-256-GCM / RSA-4096 / NIST P-256 cryptographic vaults, tamper-evident SHA-256 hash chaining, and Caddy reverse proxy integration.")

sec("1.3 Objectives & Scope")
subsec("Specific Objectives")
bullet("Zero-Knowledge Confidentiality: Ensure plaintext data and symmetric keys never reach the backend network, server memory, or persistent storage.")
bullet("Client-Side Cryptographic Pipeline: Implement client-side AES-256-GCM authenticated encryption, ensuring confidentiality and tamper detection.")
bullet("Public-Key Cryptographic Vault: Generate and register client-side RSA-4096 key pairs for asymmetric key wrapping and NIST P-256 elliptic curve key pairs for digital signatures.")
bullet("Authenticity and Non-Repudiation: Verify client-generated ECDSA digital signatures on the backend before any encrypted payload is stored.")
bullet("Multi-Recipient File Sharing: Enable users to securely share files by wrapping symmetric AES keys with each recipient's registered RSA public key.")
bullet("Expiring & Revocable Share Links: Provide cryptographic share tokens supporting configurable TTL expiration and instantaneous owner revocation.")
bullet("Tamper-Evident Audit Logging: Construct an immutable audit service where every event is linked via a cryptographic SHA-256 hash chain.")

sec("1.4 Hardware & Software Requirements")
bullet("Development Workstation: Intel Core i5 / AMD Ryzen 5 or higher, 8 GB RAM minimum, 256 GB SSD storage.")
bullet("Production Server: ARM64 / x86_64 Cloud VM (Oracle Cloud Always-Free Ampere A1), 2 vCPUs, 2 GB to 4 GB RAM, 50 GB NVMe SSD block storage.")
bullet("Software Stack: Java 21 LTS (Temurin), Spring Boot 3.3.4, PostgreSQL 16, Redis 7, MinIO S3, React 18, TypeScript, Tailwind CSS, Caddy, Docker.")

sec("1.5 Tools & Technologies Used")
p("The platform integrates Java 21, Spring Boot 3.3.4 microservices, PostgreSQL 16 databases with Flyway migrations, MinIO object storage for opaque ciphertexts, Redis 7 for rate limiting and token blacklisting, W3C Web Crypto API and Bouncy Castle for client-side cryptography, React 18 for the responsive web client, and Caddy for TLS termination and reverse proxying.")

sec("1.6 Organization of Report")
p("This report is systematically organized into eight chapters detailing requirements analysis, microservice architecture, database modeling, cryptographic algorithms, project planning, input/output designs, quality assurance, and future enhancements.")

print("Writing Chapter 2...")
chap(2, "System Analysis & Requirements Specifications")
sec("2.1 Identification of Need")
p("In modern collaborative environments, data transmission and cloud storage represent the largest surface area for security exploits. Standard cloud providers hold decryption keys, exposing customer files to internal breaches and platform exploits. SecureFileShare eliminates this dependency through mathematical zero-knowledge privacy.")

sec("2.2 Preliminary Investigation")
p("A rigorous preliminary investigation evaluated standard cloud drives (Google Drive, Dropbox), proprietary zero-knowledge clouds (Mega, Proton Drive), and decentralized PGP email transfers, identifying the need for a microservice platform that bridges client-side cryptographic security with modern web user experience.")

sec("2.3 Feasibility Study")
p("The system is technically feasible using W3C Web Crypto and Spring Boot 3, economically feasible using 100% open-source software, and operationally viable through background key management.")

sec("2.4 Functional Requirements")
tbl("Table 2.1: Functional Requirements of SecureFileShare", ["Req ID", "Requirement Title", "Detailed Description"], [
    ["FR-01", "User Registration & Auth", "Register users with unique handle, BCrypt password hashing, issue signed JWTs."],
    ["FR-02", "Public Key Registry", "Store and serve user public keys (RSA-4096 / EC P-256) with SHA-256 fingerprints."],
    ["FR-03", "Client-Side Encryption", "Generate 256-bit AES key per file; encrypt locally via AES-256-GCM."],
    ["FR-04", "RSA Key Wrapping", "Wrap AES key using recipient RSA-4096 public key with OAEP padding."],
    ["FR-05", "Digital Signature", "Compute SHA-256 hash of ciphertext; sign hash using NIST P-256 private key."],
    ["FR-06", "Signature Verification", "file-service validates SHA-256 checksum and ECDSA signature against public key."],
    ["FR-07", "Object Storage Streaming", "Stream encrypted ciphertext directly to MinIO without server memory buffering."],
    ["FR-08", "Multi-Recipient Sharing", "Wrap symmetric AES keys for multiple recipients (POST /api/v1/files/{id}/keys)."],
    ["FR-09", "Expiring & Revocable Links", "Generate unique cryptographic tokens with TTL expiration and owner revocation."],
    ["FR-10", "Tamper-Evident Audit Log", "Record all lifecycle events into an append-only, SHA-256 hash-chained log."]
])

sec("2.5 Non-Functional Requirements")
tbl("Table 2.2: Non-Functional Requirements of SecureFileShare", ["S. No.", "Quality Attribute", "Technical Requirement"], [
    ["1", "Confidentiality", "Zero-knowledge design: Plaintext files and AES keys never accessible to backend."],
    ["2", "Integrity", "Cryptographic verification via AES-GCM tags, SHA-256 hashes, and ECDSA signatures."],
    ["3", "Non-Repudiation", "Every uploaded package carries a verifiable ECDSA signature proving ownership."],
    ["4", "Performance", "Streaming uploads support up to 100 MB with p95 API response times under 500ms."],
    ["5", "Scalability", "Decoupled microservice architecture allowing independent scaling of storage and auth nodes."]
])

sec("2.6 Use Case Diagram")
p("The Use Case Diagram models user interaction with the platform boundary:")
fig("fig1_use_case_overview.png", "Figure 2.1: Use Case Diagram of SecureFileShare Platform",
    "Figure 2.1 models the core operations accessible to authenticated users: Register/Login, Manage Keys (RSA/EC), Upload Encrypted File, Download Decrypted File, Create Share Link (HMAC), Revoke Share Link, and View Audit Log. Core workflows are fully implemented; automated group vault delegations represent planned architectural extensions.", width=4.0)

sec("2.7 Data Flow Diagram")
p("DFD Level 0 models the overall system interaction between File Owners, Recipients, MinIO S3, and PostgreSQL. DFD Level 1 decomposes the system into four autonomous microservices: Auth & Key Management (8081), Zero-Knowledge Ingestion (8082), Sharing & Access Control (8083), and Tamper-Evident Audit (8084).")

sec("2.8 Sequence Diagram")
p("The Sequence Diagram details the exact cryptographic and API call sequence:")
fig("fig3_sequence_upload_pipeline.png", "Figure 2.2: Sequence Diagram of Recipient Ciphertext Upload and Key Sharing Pipeline",
    "Figure 2.2 illustrates the 11-step cryptographic sequence: File selection, random AES key generation, AES-256-GCM local encryption, recipient public key lookup, RSA-4096-OAEP key wrapping, SHA-256 hashing and ECDSA signing, multipart upload to file-service, signature verification, MinIO S3 streaming, and PostgreSQL metadata storage. Crucially, the server never sees plaintext or unwrapped keys.", width=5.5)

sec("2.9 Activity Diagram")
p("The Activity Diagram models decision branches governing uploads and downloads:")
fig("fig2_storage_workflow_activity.png", "Figure 2.3: Activity Diagram: Encrypted File Storage and Retrieval Flow",
    "Figure 2.3 models the decision workflows: The upload branch executes local encryption, key wrapping, POST /api/v1/files, and signature verification (rejecting invalid signatures with HTTP 400). The download branch queries metadata, streams ciphertext from MinIO, returns the package, and unwraps/decrypts the file locally in browser memory.", width=3.8)

print("Writing Chapter 3...")
chap(3, "System Design")
sec("3.1 System Architecture")
p("SecureFileShare is architected as a decoupled microservice platform across independent operational layers:")
fig("fig4_client_service_arch.png", "Figure 3.1: Client-Service Monorepo Architecture of SecureFileShare",
    "Figure 3.1 depicts the structural decomposition of the Maven monorepo: client-app (Desktop Client) and web-app (React SPA) communicate over HTTP APIs with audit-service (8084), auth-service (8081), file-service (8082), and share-service (8083), which all depend on the common DTO library.", width=5.2)

p("Figure 3.2 illustrates internal service component delegations and storage topology:")
fig("fig5_component_storage_topology.png", "Figure 3.2: Detailed Microservice Component & Storage Topology",
    "Figure 3.2 details the internal mechanics of each microservice: Web App Router routes to Web Crypto API; Desktop Client routes to CryptoEngine; file-service delegates to FileService.java for MinIO streaming and signature checks; audit-service computes SHA-256 hash chains; auth-service delegates to KeyValidator and AuthService with Redis rate limiting.", width=5.6)

tbl("Table 3.1: Communication Flow Between System Layers", ["Step", "From", "To", "Description"], [
    ["1", "User Client", "Caddy Proxy", "Transmits HTTPS request with JWT authorization header."],
    ["2", "Caddy Proxy", "Microservice", "Routes request to port 8081, 8082, 8083, or 8084."],
    ["3", "file-service", "auth-service", "Resolves recipient public keys for key wrapping validation."],
    ["4", "file-service", "MinIO S3", "Streams opaque encrypted ciphertext directly to S3 bucket."],
    ["5", "Microservices", "PostgreSQL", "Executes ACID relational transactions via Spring Data JPA."],
    ["6", "auth-service", "Redis Cache", "Enforces rate limits and checks JWT revocation blacklist."]
])

sec("3.2 Entity Relationship (ER) Model")
p("The ER model specifies database structures and relational cardinalities:")
fig("fig6_database_er_diagram.png", "Figure 3.3: Database Entity Relationship (ER) Diagram",
    "Figure 3.3 shows the relational schema: USERS (id PK, username UK, email UK, public_key_rsa, public_key_ec) owns (1:N) FILE_RECORDS. FILE_RECORDS (id PK, owner_id FK, ciphertext_path, wrapped_key, signature) is shared via (1:N) SHARE_RECORDS. AUDIT_RECORDS stores append-only compliance logs linked by prev_hash and current_hash.", width=4.6)

tbl("Table 3.2: Entities and Attributes of SecureFileShare System", ["Entity Name", "Key Attributes", "Description"], [
    ["USERS", "id (PK), username, email, password_hash, public_key_rsa, public_key_ec", "Stores credentials and active public keys."],
    ["KEY_REGISTRY", "id (PK), user_id (FK), rsa_public_key, ec_public_key, rsa_fingerprint", "Maintains historical rotated public keys."],
    ["FILES", "id (PK), owner_id, original_name, ciphertext_sha256, iv, object_key", "Tracks encrypted packages and MinIO S3 object paths."],
    ["FILE_KEYS", "id (PK), file_id (FK), recipient_id, wrapped_key", "Stores AES keys wrapped with recipient RSA keys."],
    ["SHARES", "id (PK), file_id, owner_id, share_token, expires_at, status", "Governs expiring and revocable share links."],
    ["AUDIT_ENTRIES", "id (PK), timestamp, actor, action, file_name, hash, previous_hash", "Append-only, SHA-256 hash-chained audit log."]
])

sec("3.3 Database Design")
p("The database enforces microservice isolation through separate PostgreSQL instances (auth_db, file_db, share_db, audit_db) managed via Flyway migrations.")

sec("3.4 Modularization Details")
p("The system is divided into modular services: Authentication Module (auth-service), Key Registry Module, File Ingestion Module (file-service), Sharing Module (share-service), Audit Module (audit-service), and Client Cryptography Modules.")

sec("3.5 Algorithm Design")
subsec("Algorithm 1: Client AES-256-GCM Encryption")
p("Generates a single-use 256-bit AES key and 12-byte IV, encrypts the file via AES-GCM, computes SHA-256 hash of ciphertext, signs the hash using NIST P-256 EC private key, and wraps the AES key with RSA-4096-OAEP.")

subsec("Algorithm 2: Server Ingestion & Signature Verification")
p("Validates JWT, retrieves sender's EC public key, computes SHA-256 hash of incoming ciphertext stream, verifies ECDSA digital signature, streams ciphertext directly to MinIO, and persists metadata in PostgreSQL.")

subsec("Algorithm 3: Audit Hash Chain Verification")
p("Queries audit_entries ordered by ID ASC, asserts previous_hash links to predecessor hash, recalculates SHA-256 block hash, and flags any database tampering.")

sec("3.6 User Interface Design")
tbl("Table 3.4: User Interface Components and Description", ["Screen", "Framework", "Operational Function"], [
    ["LoginPage", "React / Swing", "Authenticates credentials and issues signed JWT."],
    ["RegisterPage", "React / Swing", "Generates RSA-4096 / NIST P-256 key pairs locally."],
    ["FilesPage", "React / Swing", "Displays personal encrypted vault with download/share actions."],
    ["UploadPage", "React / Swing", "Orchestrates client-side encryption and streaming upload."],
    ["SharesPage", "React / Swing", "Manages expiring share links with instantaneous revocation."],
    ["AuditPage", "React / Swing", "Displays compliance events and executes hash chain verification."]
])

sec("3.7 Data Integrity & Constraints")
p("Enforces entity integrity (UUID PKs), referential integrity (cascading deletes), domain integrity (strict enumeration columns), and cryptographic integrity (GCM authentication tags, ECDSA signatures, and SHA-256 hash chains).")

sec("3.8 Software Engineering Paradigm")
p("Follows a Hybrid Software Engineering Model combining Waterfall planning for requirements and cryptographic modeling with Iterative development for microservice implementation and automated CI/CD verification.")

print("Writing Chapter 4...")
chap(4, "Project Management")
sec("4.1 Project Planning & Scheduling")
tbl("Table 4.1: Work Breakdown Structure (WBS)", ["Phase", "Task Name", "Engineering Focus"], [
    ["Phase 1", "Requirements & Crypto Modeling", "Zero-knowledge constraint definition, crypto selection."],
    ["Phase 2", "Architecture & DB Design", "Maven scaffolding, Flyway migrations, PostgreSQL schemas."],
    ["Phase 3", "Auth Service & Key Registry", "JWT authentication, BCrypt, public key registry, rate limiting."],
    ["Phase 4", "File Service & MinIO Streaming", "Streaming upload pipelines, SHA-256 and ECDSA checks."],
    ["Phase 5", "Share Service & Access Control", "Expiring share tokens, recipient key wrapping, revocation."],
    ["Phase 6", "Audit Service & Hash Chaining", "Append-only event ingestion, SHA-256 hash chain verification."],
    ["Phase 7", "Dual Client Development", "React 18 Web Crypto SPA and Java Swing desktop client."],
    ["Phase 8", "CI/CD & Production Deploy", "GitHub Actions, Gitleaks, OWASP scanning, Caddy TLS deploy."]
])

sec("4.2 Milestones & Deliverables")
tbl("Table 4.3: Project Milestones", ["Milestone", "Description", "Verification Criterion"], [
    ["M1", "Cryptographic Specification", "Zero-knowledge mathematical flow approved."],
    ["M2", "Database & Schema Scaffolding", "Flyway migrations apply cleanly to 4 databases."],
    ["M3", "Authentication & Key Registry", "Users register, authenticate, store public keys."],
    ["M4", "Zero-Knowledge File Ingestion", "Ciphertexts stream to MinIO; ECDSA signatures verify."],
    ["M5", "Share Links & Hash Chain", "Share links expire/revoke; audit chain verifies intact."],
    ["M6", "Dual-Client Operation", "Web and desktop clients interoperate seamlessly."],
    ["M7", "Automated CI/CD Deployment", "Gitleaks, Maven verify, and Docker push execute cleanly."]
])

sec("4.3 Risk Management")
tbl("Table 4.5: Risk Analysis & Impact Matrix", ["Identified Risk", "Impact", "Probability", "Mitigation Strategy"], [
    ["Plaintext Leakage to Server", "Critical", "Low", "Strict DTO validation rejecting unencrypted fields; client keys."],
    ["Cross-Client Crypto Drift", "High", "Medium", "Shared test vectors validating Web Crypto against Bouncy Castle."],
    ["Audit Hash Chain Drift", "High", "Medium", "Pessimistic locking (SELECT ... FOR UPDATE) on append."],
    ["Upload Memory Exhaustion", "High", "Medium", "Direct reactive streaming to MinIO without JVM heap buffering."],
    ["Brute-Force Token Guessing", "High", "Low", "128-bit high-entropy cryptographic random share tokens."],
    ["Credential Leakage in Git", "High", "Low", "Automated Gitleaks secret scanning on every git push via CI."]
])

sec("4.4 Cost Estimation")
tbl("Table 4.6: Cost Estimation of the Project", ["Component Category", "Selected Technology & Tooling", "Estimated Cost"], [
    ["Hardware & Workstation", "Existing Development PC / Laptop", "₹0 (Owned)"],
    ["Backend Framework", "Java 21 LTS & Spring Boot 3.3.4 (Open-Source)", "₹0"],
    ["Database & Caching", "PostgreSQL 16 & Redis 7 (Open-Source)", "₹0"],
    ["Object Storage", "MinIO S3 Compatible Community Edition", "₹0"],
    ["Frontend & Desktop", "React 18, Vite, Tailwind CSS, Java Swing FlatLaf", "₹0"],
    ["Cloud Infrastructure", "Oracle Cloud Infrastructure Always-Free Tier", "₹0"],
    ["SSL/TLS Certificates", "Automated Let’s Encrypt via Caddy", "₹0"],
    ["CI/CD Automation", "GitHub Actions Public Repository Runners", "₹0"],
    ["TOTAL PROJECT EXPENSE", "Full-Stack Zero-Knowledge Platform", "₹0 (Zero Cost)"]
])

sec("4.5 PERT Chart & Critical Path")
p("Critical Path: Requirements (1W) -> System Design (1W) -> Database Migrations (1W) -> Backend File Service (1.5W) -> Client Cryptography & Web App (2W) -> Testing (1W) -> Production Deployment (0.5W). Total Project Duration: 8 Weeks.")

sec("4.6 Gantt Chart")
p("The Gantt schedule maps parallel activities: Auth and File microservices were engineered concurrently during Weeks 3 and 4; Web and Desktop client interfaces were built concurrently in Weeks 5 and 6, followed by integration testing and deployment.")

sec("4.7 Continuous Integration & Deployment Pipeline")
p("The automated release pipeline executes continuous testing and cloud deployment:")
fig("fig7_cicd_release_pipeline.png", "Figure 4.1: Continuous Integration and Continuous Deployment (CI/CD) Pipeline",
    "Figure 4.1 details the two-stage GitHub Actions pipeline: Continuous Integration (ci.yml) executes Gitleaks secret scanning, sets up Java 21 Temurin with Maven caching, runs ./mvnw -B verify across all modules, runs OWASP Dependency-Check, and archives test reports. Continuous Deployment (deploy.yml) builds ARM64 multi-stage images, pushes to GHCR, connects via SSH to Oracle Cloud VM, synchronizes compose files, restarts containers, and executes health probes on ports 8081-8084 (automatically rolling back if health checks fail).", width=3.8)

print("Writing Chapters 5, 6, 7, 8 & References...")
# CHAPTER 5: INPUT DESIGN
chap(5, "Input Design")
sec("5.1 User Registration & Key Pair Generation Form")
p("Captures username, email, master password, and coordinates client-side RSA-4096 and NIST P-256 key pair generation via W3C Web Crypto, storing private keys in IndexedDB and transmitting public keys.")

sec("5.2 Zero-Knowledge File Upload & Encryption Interface")
p("Accepts files up to 100 MB, encrypts via single-use AES-256-GCM key in local memory, wraps AES key with RSA-4096-OAEP, signs ciphertext SHA-256 hash with ECDSA, and streams multipart package to file-service.")

sec("5.3 Encrypted File Share & Recipient Key-Wrapping Form")
p("Enables owners to look up recipient RSA public keys, re-wrap the symmetric AES key, and issue time-limited share links.")

sec("5.4 Secure Key Backup, Export & Passphrase Protection Form")
p("Derives key encryption key via PBKDF2 (600,000 iterations, SHA-256), encrypts private keys, and exports an encrypted JSON keystore.")

# CHAPTER 6: OUTPUT DESIGN
chap(6, "Output Design")
sec("6.1 Encrypted File Vault & Metadata Dashboard (Table View)")
tbl("Table 6.1: Sample Encrypted Vault File Records", ["File Name", "Size", "Ciphertext SHA-256 Hash", "Status", "Upload Date"], [
    ["thesis_draft.pdf", "4.2 MB", "e3b0c44298fc1c149afbf4c8996fb92427...", "CLEAN", "2026-11-18"],
    ["financial_q3.xlsx", "1.8 MB", "7f83b1657ff1fc53b92dc18148a1d65dfc...", "CLEAN", "2026-11-17"],
    ["source_code.zip", "18.5 MB", "185f8db32271fe25f561a6fc938b2e2643...", "CLEAN", "2026-11-15"]
])

sec("6.2 Tamper-Evident Audit Trail & Hash-Chain Verification Log")
tbl("Table 6.2: Sample Audit Trail & Hash-Chain Verification Log", ["ID", "Timestamp (UTC)", "Actor", "Action", "Current Hash", "Previous Hash"], [
    ["1", "2026-11-17 08:00", "system", "SYSTEM_INIT", "c4ca4238a0...", "0000000000..."],
    ["2", "2026-11-17 08:15", "ayush", "USER_REGISTER", "c81e728d9d...", "c4ca4238a0..."],
    ["3", "2026-11-17 08:30", "ayush", "UPLOAD", "eccbc87e4b...", "c81e728d9d..."],
    ["4", "2026-11-17 09:12", "ayush", "SHARE_CREATED", "a87ff679a2...", "eccbc87e4b..."]
])

# CHAPTER 7: TESTING
chap(7, "Testing & Quality Assurance")
sec("7.1 Testing Strategy")
p("Testing follows an automated multi-tier pyramid covering unit tests, integration tests, security test cases, and end-to-end system validation. Automated builds execute via ./mvnw -B verify across all modules, achieving 100% green pass results across unit and integration suites.")

sec("7.2 Unit Testing")
tbl("Table 7.1: Unit Testing Details & Validation Results", ["Module", "Target Component", "Test Condition", "Expected Result", "Status"], [
    ["auth-service", "JwtService", "Valid claims & secret key", "Signed JWT issued cleanly", "PASS"],
    ["auth-service", "JwtService", "Expired token supplied", "TokenExpiredException thrown", "PASS"],
    ["auth-service", "RateLimiterService", "Exceeding 5 logins/min", "HTTP 429 Rate Limit returned", "PASS"],
    ["auth-service", "KeyRegistryService", "Valid RSA-4096 & EC keys", "Keys registered; fingerprints generated", "PASS"],
    ["file-service", "CryptoEngine", "AES-256-GCM round trip", "Decrypted bytes match plaintext", "PASS"],
    ["file-service", "SignatureVerifier", "Valid NIST P-256 signature", "Signature verification returns true", "PASS"],
    ["file-service", "SignatureVerifier", "Tampered ciphertext payload", "Signature verification returns false", "PASS"],
    ["share-service", "ShareService", "Lookup active share token", "Valid share record returned", "PASS"],
    ["share-service", "ShareService", "Lookup revoked share token", "ShareAccessDeniedException thrown", "PASS"],
    ["audit-service", "HashChainService", "Append sequential records", "Deterministic unbroken SHA-256 chain", "PASS"],
    ["web-app", "webCryptoService", "4096-bit RSA key generation", "CryptoKey handles stored in IndexedDB", "PASS"]
])

sec("7.3 Integration Testing")
p("Integration tests verify that Flyway database migrations apply cleanly across all four PostgreSQL schemas, MinIO streaming correctly transfers multi-megabyte binary payloads, and Redis enforces token blacklisting upon logout.")

sec("7.4 System Testing")
p("Validates end-to-end workflows: user registration, key pair exchange, client-side encryption, upload to MinIO, recipient key wrapping, expiring link access, download, and local decryption.")

sec("7.5 Test Cases")
tbl("Table 7.2: System Test Cases & Security Validation Results", ["TC ID", "Test Scenario", "Input Conditions", "Expected Result", "Result"], [
    ["TC-01", "Valid Registration", "Unique handle, valid email, strong password", "Account created; keys registered; 201 Created", "PASS"],
    ["TC-02", "Duplicate Username", "Existing handle submitted during registration", "HTTP 409 Conflict returned", "PASS"],
    ["TC-03", "Valid JWT Auth", "Correct username and password", "Signed JWT returned with 1-hr expiry", "PASS"],
    ["TC-04", "Invalid Password", "Incorrect password supplied", "HTTP 401 Unauthorized returned", "PASS"],
    ["TC-05", "Zero-Knowledge Upload", "Ciphertext stream + valid ECDSA signature", "Stored in MinIO; metadata saved; 201 Created", "PASS"],
    ["TC-06", "Tampered Ciphertext", "Payload altered during transmission", "Signature mismatch; HTTP 400 returned", "PASS"],
    ["TC-07", "Unauthorized Access", "User requests file without key grant", "HTTP 403 Forbidden returned", "PASS"],
    ["TC-08", "Recipient Key Wrap", "Owner wraps AES key for Recipient B", "Key stored in file_keys; Recipient B can decrypt", "PASS"],
    ["TC-09", "Expired Share Link", "Accessing share link after TTL deadline", "HTTP 410 Gone / 403 Forbidden", "PASS"],
    ["TC-10", "Revoked Share Link", "Owner revokes active share link", "Status = REVOKED; subsequent access denied", "PASS"],
    ["TC-11", "Audit Chain Integrity", "Admin invokes /api/v1/audit/verify", "Returns { valid: true, brokenEntryId: null }", "PASS"],
    ["TC-12", "Audit Row Tampering", "Row modified directly via SQL query", "Verification flags break; reports exact row ID", "PASS"]
])

sec("7.6 Security Measures")
p("Security hardening enforces client-isolated private keys in IndexedDB, stateless JWT with Redis logout blacklisting, and Caddy TLS 1.3 termination with strict CSP, HSTS, and CORS headers.")

sec("7.7 Database Security")
p("Dedicated PostgreSQL instances per service, version-controlled Flyway migrations, parameterized queries via Hibernate, and credentials passed strictly via environment variables.")

sec("7.8 User Roles & Access Control")
tbl("Table 7.3: User Roles and Access Permissions Matrix", ["User Role", "Operational Responsibilities", "Permitted Endpoints"], [
    ["ROLE_USER", "Personal vault management, encryption, upload, sharing, revocation, key backup.", "/api/v1/files/**, /api/v1/shares/**, /api/v1/keys/**"],
    ["ROLE_ADMIN", "Platform monitoring, administrative audit log inspection, audit hash chain verification.", "/api/v1/audit/**, /api/v1/health/**, /actuator/**"]
])

# CHAPTER 8: SUMMARY & FUTURE SCOPE
chap(8, "Summary & Future Scope")
sec("8.1 Summary")
p("SecureFileShare provides a production-ready, zero-knowledge encrypted file sharing and vault platform. By eliminating the trusted-server assumption, the platform ensures that even a catastrophic breach of server infrastructure reveals only opaque, mathematically undecryptable ciphertexts.")

sec("8.2 Future Enhancements")
bullet("Native Mobile Applications (iOS / Android): Native mobile clients with biometric hardware keychain integration (Apple Secure Enclave, Android Keystore).")
bullet("End-to-End Encrypted Group Workspaces: Decentralized group key management using Messaging Layer Security (MLS) for collaborative encrypted folders.")
bullet("WebRTC Direct Peer-to-Peer Transfers: Direct browser-to-browser encrypted transfers for massive files, bypassing cloud storage entirely.")
bullet("Post-Quantum Cryptography (PQC): Transitioning to NIST-approved post-quantum algorithms (ML-KEM and ML-DSA).")
bullet("Automated Compliance Reporting: One-click export of cryptographically validated audit trails to simplify enterprise GDPR and HIPAA audits.")

# REFERENCES
doc.add_page_break()
p("REFERENCES", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False)
references = [
    "National Institute of Standards and Technology (NIST). (2001). Advanced Encryption Standard (AES). Federal Information Processing Standards Publication (FIPS PUB 197). https://doi.org/10.6028/NIST.FIPS.197",
    "Dworkin, M. (2007). Recommendation for Block Cipher Modes of Operation: Galois/Counter Mode (GCM) and GMAC. NIST Special Publication 800-38D. https://doi.org/10.6028/NIST.SP.800-38D",
    "Moriarty, K., Kaliski, B., Jonsson, J., & Rusch, A. (2016). PKCS #1: RSA Cryptography Specifications Version 2.2. RFC 8017, Internet Engineering Task Force (IETF). https://doi.org/10.17487/RFC8017",
    "Barker, E. (2020). Recommendation for Key Management: Part 1 – General. NIST Special Publication 800-57 Part 1, Rev. 5. https://doi.org/10.6028/NIST.SP.800-57pt1r5",
    "World Wide Web Consortium (W3C). (2017). Web Cryptography API - W3C Recommendation. https://www.w3.org/TR/WebCryptoAPI/",
    "OWASP Foundation. (2024). Cryptographic Storage Cheat Sheet. OWASP Cheat Sheet Series. https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html",
    "OWASP Foundation. (2024). JSON Web Token for Java Cheat Sheet. OWASP Cheat Sheet Series. https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html",
    "Walls, C. (2022). Spring in Action (6th ed.). Manning Publications.",
    "Burtch, K. (2021). Linux System Administration: A Guide to Enterprise Computing. O’Reilly Media.",
    "Haber, S., & Stornetta, W. S. (1991). How to time-stamp a digital document. Journal of Cryptology, 3(2), 99–111. https://doi.org/10.1007/BF00196791"
]
for r in references:
    para = doc.add_paragraph(style='List Number')
    para.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    r_run = para.add_run(r)
    r_run.font.name = 'Times New Roman'
    r_run.font.size = Pt(10)

print("Saving complete DOCX document...")
doc.save(DOCX_OUT)
print(f"SUCCESS: DOCX generated at {DOCX_OUT} ({os.path.getsize(DOCX_OUT):,} bytes)")

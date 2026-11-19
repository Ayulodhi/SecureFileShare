# -*- coding: utf-8 -*-
"""
sections_prelim.py
Preliminary pages: Cover, Declaration, Certificate, Acknowledgement, TOC, Abbreviations, Figures, Tables
"""

def get_prelim_html():
    return """
<!-- ==================== COVER PAGE ==================== -->
<div class="cover-container page-break">
    <div>
        <h1 class="cover-main-title">PROJECT REPORT ON</h1>
        <div class="double-divider"></div>
        <h2 class="cover-proj-title">SecureFileShare – Zero-Knowledge Encrypted File Sharing &amp; Vault Platform</h2>
        <div class="double-divider"></div>
        <p class="cover-text no-indent" style="text-align: center;">Submitted to Department of Computer Applications in partial fulfillment for the award of the degree of</p>
        <div class="degree-label">MASTER OF COMPUTER APPLICATIONS</div>
        <div class="batch-label">Batch (2025–2027)</div>
    </div>

    <div class="meta-split">
        <table style="width: 100%; border: none;">
            <tr>
                <td style="width: 50%; text-align: left; border: none; font-size: 11pt; vertical-align: top;">
                    <strong>Submitted by:</strong><br><br>
                    <strong>Ayush Lodhi</strong><br>
                    GE - 25113399
                </td>
                <td style="width: 50%; text-align: right; border: none; font-size: 11pt; vertical-align: top;">
                    <strong>Under the Guidance of:</strong><br><br>
                    <strong>Project Supervisor / Faculty Guide</strong><br>
                    (Professor / Associate Professor)
                </td>
            </tr>
        </table>
    </div>

    <div>
        <div class="univ-emblem">
            <span>GEU<br>MCA</span>
        </div>
        <div class="dept-title">Department of Computer Applications</div>
        <div class="univ-title">Graphic Era (Deemed to be University)</div>
        <div class="location-title">Dehradun, Uttarakhand<br>November-2026</div>
    </div>
</div>

<!-- ==================== CANDIDATE DECLARATION ==================== -->
<div class="page-break">
    <div style="text-align: right; font-size: 10pt; font-style: italic;">ii</div>
    <div style="text-align: center; margin-bottom: 25px;">
        <div class="univ-title" style="font-size: 15pt;">Graphic Era (Deemed to be University)</div>
        <div style="font-size: 10.5pt; color: #555;">Accredited by NAAC with Grade 'A+' | Dehradun, Uttarakhand</div>
    </div>
    <h2 class="chap-header" style="margin-top: 15px; border: none;">CANDIDATE’S DECLARATION</h2>
    <div style="height: 2px; background-color: #0d3b66; width: 60px; margin: 0 auto 25px auto;"></div>

    <p>I hereby certify that the work presented in this project report entitled <strong>&ldquo;SecureFileShare &ndash; Zero-Knowledge Encrypted File Sharing &amp; Vault Platform&rdquo;</strong> in partial fulfilment of the requirements for the award of the degree of <strong>Master of Computer Applications</strong> is a bonafide work carried out by me during the period of August 2026 to November 2026 under the supervision of Faculty Guide, Department of Computer Applications, Graphic Era Deemed to be University, Dehradun, India.</p>

    <p>This work has not been submitted elsewhere for the award of any other degree, diploma, or certificate.</p>

    <div style="margin-top: 50px; text-align: right;">
        <div style="display: inline-block; text-align: center;">
            <div style="height: 35px;"></div>
            <strong>Ayush Lodhi</strong><br>
            Name and Signature of Candidate
        </div>
    </div>

    <p class="no-indent" style="margin-top: 35px;">This is to certify that the above mentioned statement in the candidate&rsquo;s declaration is correct to the best of my knowledge.</p>

    <table class="signature-table" style="margin-top: 25px;">
        <tr>
            <td style="width: 50%; border: none;">
                <strong>Date:</strong> ____________________
            </td>
            <td style="width: 50%; text-align: right; border: none;">
                <div class="sig-line" style="margin-top: 10px; margin-left: auto;">Name and Signature of Guide</div>
            </td>
        </tr>
        <tr>
            <td style="width: 50%; border: none; padding-top: 45px;">
                <div class="sig-line">Signature of Supervisor</div>
            </td>
            <td style="width: 50%; text-align: right; border: none; padding-top: 45px;">
                <div class="sig-line" style="margin-left: auto;">Signature of External Examiner</div>
            </td>
        </tr>
    </table>

    <div style="text-align: center; margin-top: 40px;">
        <strong>HOD</strong><br>
        Department of Computer Applications
    </div>
</div>

<!-- ==================== CERTIFICATE OF ORIGINALITY ==================== -->
<div class="page-break">
    <div style="text-align: right; font-size: 10pt; font-style: italic;">iii</div>
    <h2 class="chap-header" style="margin-top: 20px; border: none;">CERTIFICATE OF ORIGINALITY</h2>
    <div style="height: 2px; background-color: #0d3b66; width: 60px; margin: 0 auto 25px auto;"></div>

    <p>This is to certify that the project report entitled <strong>&ldquo;SecureFileShare &ndash; Zero-Knowledge Encrypted File Sharing &amp; Vault Platform&rdquo;</strong> submitted to <strong>Graphic Era University, Dehradun</strong> in partial fulfilment of the requirement for the award of the degree of <strong>MASTER OF COMPUTER APPLICATIONS (MCA)</strong>, is an authentic and original work carried out by <strong>Mr. Ayush Lodhi</strong> with enrolment number <strong>GE-25113399</strong> under my supervision and guidance.</p>

    <p>The matter embodied in this project is genuine work done by the student and has not been submitted whether to this University or to any other University / Institute for the fulfilment of the requirements of any course of study.</p>

    <table class="signature-table" style="margin-top: 45px;">
        <tr>
            <td style="width: 50%; border: none;">
                <div class="sig-line">Signature of the Student</div>
                <div style="margin-top: 8px; font-size: 10pt;">Date: ________________________</div>
            </td>
            <td style="width: 50%; text-align: right; border: none;">
                <div class="sig-line" style="margin-left: auto;">Signature of the Guide</div>
                <div style="margin-top: 8px; font-size: 10pt;">Date: ________________________</div>
            </td>
        </tr>
        <tr>
            <td style="border: none; padding-top: 25px; font-size: 10pt;">
                <strong>Enrolment No.:</strong> GE-25113399<br><br>
                <strong>Name and Address of Student:</strong><br>
                Ayush Lodhi<br>
                Department of Computer Applications<br>
                Graphic Era Deemed to be University, Dehradun
            </td>
            <td style="border: none; padding-top: 25px; text-align: right; font-size: 10pt;">
                <strong>Name, Designation and Address of Guide:</strong><br><br>
                Faculty Guide (Professor)<br>
                Department of Computer Applications<br>
                Graphic Era Deemed to be University, Dehradun
            </td>
        </tr>
    </table>

    <div style="margin-top: 35px; font-size: 10pt; border-top: 1px dashed #aaa; padding-top: 10px;">
        <strong>Special Note:</strong> All client-side cryptographic modules, microservice backends, MinIO object streaming pipelines, and automated CI/CD configurations documented in this report represent verified technical implementations.
    </div>
</div>

<!-- ==================== ACKNOWLEDGEMENT ==================== -->
<div class="page-break">
    <div style="text-align: right; font-size: 10pt; font-style: italic;">iv</div>
    <h2 class="chap-header" style="margin-top: 20px; border: none;">ACKNOWLEDGEMENT</h2>
    <div style="height: 2px; background-color: #0d3b66; width: 60px; margin: 0 auto 25px auto;"></div>

    <p>First of all, I would like to express my sincere and wholehearted gratitude to my project guide, Faculty Guide, Department of Computer Applications, Graphic Era (Deemed to be University), for invaluable guidance, continuous support, and encouragement throughout the development of this project. Her insightful suggestions and constant motivation helped me to successfully complete this work.</p>

    <p>I would also like to thank all the faculty members of the Department of Computer Applications for providing me with the necessary knowledge, resources, and a supportive academic environment that greatly contributed to the successful completion of this project.</p>

    <p>I am grateful to my friends and colleagues for their cooperation, constructive suggestions, and continuous support during the entire duration of the project. Their help and encouragement played an important role in overcoming various technical challenges faced during the development of zero-knowledge cryptographic pipelines, microservice coordination, and distributed database synchronization.</p>

    <p>Last but not least, I thank everyone who directly or indirectly contributed to the successful completion of this project.</p>

    <div style="margin-top: 60px; text-align: right;">
        <div style="display: inline-block; text-align: center;">
            <div style="height: 35px;"></div>
            <strong>Name: Ayush Lodhi</strong><br>
            Signature: ___________________________
        </div>
    </div>
</div>

<!-- ==================== TABLE OF CONTENTS ==================== -->
<div class="page-break">
    <div style="text-align: right; font-size: 10pt; font-style: italic;">v</div>
    <h2 class="chap-header" style="margin-top: 20px; border: none;">TABLE OF CONTENTS</h2>
    <div style="height: 2px; background-color: #0d3b66; width: 60px; margin: 0 auto 20px auto;"></div>

    <table class="report-table" style="font-size: 10pt;">
        <thead>
            <tr>
                <th style="width: 10%;">S. No.</th>
                <th style="width: 75%; text-align: left;">Title</th>
                <th style="width: 15%;">Page No.</th>
            </tr>
        </thead>
        <tbody>
            <tr><td style="text-align: center;">1</td><td>Candidate’s Declaration</td><td style="text-align: center;">ii</td></tr>
            <tr><td style="text-align: center;">2</td><td>Certificate of Originality</td><td style="text-align: center;">iii</td></tr>
            <tr><td style="text-align: center;">3</td><td>Acknowledgement</td><td style="text-align: center;">iv</td></tr>
            <tr><td style="text-align: center;">4</td><td>Table of Contents</td><td style="text-align: center;">v</td></tr>
            <tr><td style="text-align: center;">5</td><td>List of Abbreviations</td><td style="text-align: center;">vii</td></tr>
            <tr><td style="text-align: center;">6</td><td>List of Figures</td><td style="text-align: center;">viii</td></tr>
            <tr><td style="text-align: center;">7</td><td>List of Tables</td><td style="text-align: center;">ix</td></tr>
            <tr style="font-weight: bold; background-color: #fafbfc;"><td style="text-align: center;">8</td><td>Chapter 1: Introduction</td><td style="text-align: center;">1</td></tr>
            <tr><td></td><td style="padding-left: 20px;">1.1 Introduction</td><td style="text-align: center;">1</td></tr>
            <tr><td></td><td style="padding-left: 20px;">1.2 Motivation</td><td style="text-align: center;">2</td></tr>
            <tr><td></td><td style="padding-left: 20px;">1.3 Objectives &amp; Scope</td><td style="text-align: center;">3</td></tr>
            <tr><td></td><td style="padding-left: 20px;">1.4 Hardware &amp; Software Requirements</td><td style="text-align: center;">4</td></tr>
            <tr><td></td><td style="padding-left: 20px;">1.5 Tools &amp; Technologies Used</td><td style="text-align: center;">7</td></tr>
            <tr><td></td><td style="padding-left: 20px;">1.6 Organization of Report</td><td style="text-align: center;">10</td></tr>
            <tr style="font-weight: bold; background-color: #fafbfc;"><td style="text-align: center;">9</td><td>Chapter 2: System Analysis &amp; SRS</td><td style="text-align: center;">12</td></tr>
            <tr><td></td><td style="padding-left: 20px;">2.1 Identification of Need</td><td style="text-align: center;">12</td></tr>
            <tr><td></td><td style="padding-left: 20px;">2.2 Preliminary Investigation</td><td style="text-align: center;">14</td></tr>
            <tr><td></td><td style="padding-left: 20px;">2.3 Feasibility Study</td><td style="text-align: center;">16</td></tr>
            <tr><td></td><td style="padding-left: 20px;">2.4 Functional Requirements</td><td style="text-align: center;">19</td></tr>
            <tr><td></td><td style="padding-left: 20px;">2.5 Non-Functional Requirements</td><td style="text-align: center;">20</td></tr>
            <tr><td></td><td style="padding-left: 20px;">2.6 Use Case Diagram</td><td style="text-align: center;">21</td></tr>
            <tr><td></td><td style="padding-left: 20px;">2.7 Data Flow Diagram</td><td style="text-align: center;">23</td></tr>
            <tr><td></td><td style="padding-left: 20px;">2.8 Sequence Diagram</td><td style="text-align: center;">25</td></tr>
            <tr><td></td><td style="padding-left: 20px;">2.9 Activity Diagram</td><td style="text-align: center;">27</td></tr>
            <tr style="font-weight: bold; background-color: #fafbfc;"><td style="text-align: center;">10</td><td>Chapter 3: System Design</td><td style="text-align: center;">29</td></tr>
            <tr><td></td><td style="padding-left: 20px;">3.1 System Architecture</td><td style="text-align: center;">29</td></tr>
            <tr><td></td><td style="padding-left: 20px;">3.2 ER Diagram</td><td style="text-align: center;">33</td></tr>
            <tr><td></td><td style="padding-left: 20px;">3.3 Database Design</td><td style="text-align: center;">35</td></tr>
            <tr><td></td><td style="padding-left: 20px;">3.4 Modularization Details</td><td style="text-align: center;">38</td></tr>
            <tr><td></td><td style="padding-left: 20px;">3.5 Algorithm Design</td><td style="text-align: center;">41</td></tr>
            <tr><td></td><td style="padding-left: 20px;">3.6 User Interface Design</td><td style="text-align: center;">44</td></tr>
            <tr><td></td><td style="padding-left: 20px;">3.7 Data Integrity &amp; Constraints</td><td style="text-align: center;">50</td></tr>
            <tr><td></td><td style="padding-left: 20px;">3.8 Software Engineering Paradigm</td><td style="text-align: center;">53</td></tr>
            <tr style="font-weight: bold; background-color: #fafbfc;"><td style="text-align: center;">11</td><td>Chapter 4: Project Management</td><td style="text-align: center;">55</td></tr>
            <tr><td></td><td style="padding-left: 20px;">4.1 Project Planning &amp; Scheduling</td><td style="text-align: center;">55</td></tr>
            <tr><td></td><td style="padding-left: 20px;">4.2 Milestones &amp; Deliverables</td><td style="text-align: center;">58</td></tr>
            <tr><td></td><td style="padding-left: 20px;">4.3 Risk Management</td><td style="text-align: center;">60</td></tr>
            <tr><td></td><td style="padding-left: 20px;">4.4 Cost Estimation</td><td style="text-align: center;">64</td></tr>
            <tr><td></td><td style="padding-left: 20px;">4.5 PERT Chart</td><td style="text-align: center;">66</td></tr>
            <tr><td></td><td style="padding-left: 20px;">4.6 Gantt Chart</td><td style="text-align: center;">68</td></tr>
            <tr><td></td><td style="padding-left: 20px;">4.7 CI/CD Release Pipeline</td><td style="text-align: center;">70</td></tr>
            <tr style="font-weight: bold; background-color: #fafbfc;"><td style="text-align: center;">12</td><td>Chapter 5: Input Design</td><td style="text-align: center;">72</td></tr>
            <tr style="font-weight: bold; background-color: #fafbfc;"><td style="text-align: center;">13</td><td>Chapter 6: Output Design</td><td style="text-align: center;">78</td></tr>
            <tr style="font-weight: bold; background-color: #fafbfc;"><td style="text-align: center;">14</td><td>Chapter 7: Testing &amp; Quality Assurance</td><td style="text-align: center;">81</td></tr>
            <tr style="font-weight: bold; background-color: #fafbfc;"><td style="text-align: center;">15</td><td>Chapter 8: Summary &amp; Future Scope</td><td style="text-align: center;">90</td></tr>
            <tr style="font-weight: bold; background-color: #fafbfc;"><td style="text-align: center;">16</td><td>References</td><td style="text-align: center;">94</td></tr>
        </tbody>
    </table>
</div>

<!-- ==================== LIST OF ABBREVIATIONS ==================== -->
<div class="page-break">
    <div style="text-align: right; font-size: 10pt; font-style: italic;">vii</div>
    <h2 class="chap-header" style="margin-top: 20px; border: none;">LIST OF ABBREVIATIONS</h2>
    <div style="height: 2px; background-color: #0d3b66; width: 60px; margin: 0 auto 20px auto;"></div>

    <table class="report-table">
        <thead>
            <tr>
                <th style="width: 10%;">S. No.</th>
                <th style="width: 25%;">Abbreviation</th>
                <th style="width: 50%; text-align: left;">Full Form</th>
                <th style="width: 15%;">Chapter</th>
            </tr>
        </thead>
        <tbody>
            <tr><td style="text-align: center;">1</td><td style="text-align: center;">AES</td><td>Advanced Encryption Standard</td><td style="text-align: center;">Ch. 1</td></tr>
            <tr><td style="text-align: center;">2</td><td style="text-align: center;">GCM</td><td>Galois/Counter Mode</td><td style="text-align: center;">Ch. 1</td></tr>
            <tr><td style="text-align: center;">3</td><td style="text-align: center;">RSA</td><td>Rivest–Shamir–Adleman Asymmetric Cryptosystem</td><td style="text-align: center;">Ch. 1</td></tr>
            <tr><td style="text-align: center;">4</td><td style="text-align: center;">OAEP</td><td>Optimal Asymmetric Encryption Padding</td><td style="text-align: center;">Ch. 1</td></tr>
            <tr><td style="text-align: center;">5</td><td style="text-align: center;">ECDSA</td><td>Elliptic Curve Digital Signature Algorithm</td><td style="text-align: center;">Ch. 1</td></tr>
            <tr><td style="text-align: center;">6</td><td style="text-align: center;">SHA</td><td>Secure Hash Algorithm</td><td style="text-align: center;">Ch. 1</td></tr>
            <tr><td style="text-align: center;">7</td><td style="text-align: center;">NIST</td><td>National Institute of Standards and Technology</td><td style="text-align: center;">Ch. 1</td></tr>
            <tr><td style="text-align: center;">8</td><td style="text-align: center;">JWT</td><td>JSON Web Token</td><td style="text-align: center;">Ch. 1</td></tr>
            <tr><td style="text-align: center;">9</td><td style="text-align: center;">API</td><td>Application Programming Interface</td><td style="text-align: center;">Ch. 1</td></tr>
            <tr><td style="text-align: center;">10</td><td style="text-align: center;">REST</td><td>Representational State Transfer</td><td style="text-align: center;">Ch. 1</td></tr>
            <tr><td style="text-align: center;">11</td><td style="text-align: center;">DFD</td><td>Data Flow Diagram</td><td style="text-align: center;">Ch. 2</td></tr>
            <tr><td style="text-align: center;">12</td><td style="text-align: center;">SRS</td><td>Software Requirements Specification</td><td style="text-align: center;">Ch. 2</td></tr>
            <tr><td style="text-align: center;">13</td><td style="text-align: center;">UML</td><td>Unified Modeling Language</td><td style="text-align: center;">Ch. 2</td></tr>
            <tr><td style="text-align: center;">14</td><td style="text-align: center;">ER</td><td>Entity Relationship</td><td style="text-align: center;">Ch. 3</td></tr>
            <tr><td style="text-align: center;">15</td><td style="text-align: center;">PK</td><td>Primary Key</td><td style="text-align: center;">Ch. 3</td></tr>
            <tr><td style="text-align: center;">16</td><td style="text-align: center;">FK</td><td>Foreign Key</td><td style="text-align: center;">Ch. 3</td></tr>
            <tr><td style="text-align: center;">17</td><td style="text-align: center;">ORM</td><td>Object Relational Mapping</td><td style="text-align: center;">Ch. 3</td></tr>
            <tr><td style="text-align: center;">18</td><td style="text-align: center;">JPA</td><td>Java Persistence API</td><td style="text-align: center;">Ch. 3</td></tr>
            <tr><td style="text-align: center;">19</td><td style="text-align: center;">UI</td><td>User Interface</td><td style="text-align: center;">Ch. 3</td></tr>
            <tr><td style="text-align: center;">20</td><td style="text-align: center;">UX</td><td>User Experience</td><td style="text-align: center;">Ch. 3</td></tr>
            <tr><td style="text-align: center;">21</td><td style="text-align: center;">WBS</td><td>Work Breakdown Structure</td><td style="text-align: center;">Ch. 4</td></tr>
            <tr><td style="text-align: center;">22</td><td style="text-align: center;">PERT</td><td>Program Evaluation and Review Technique</td><td style="text-align: center;">Ch. 4</td></tr>
            <tr><td style="text-align: center;">23</td><td style="text-align: center;">CI/CD</td><td>Continuous Integration / Continuous Deployment</td><td style="text-align: center;">Ch. 4</td></tr>
            <tr><td style="text-align: center;">24</td><td style="text-align: center;">GHCR</td><td>GitHub Container Registry</td><td style="text-align: center;">Ch. 4</td></tr>
            <tr><td style="text-align: center;">25</td><td style="text-align: center;">CSP</td><td>Content Security Policy</td><td style="text-align: center;">Ch. 7</td></tr>
            <tr><td style="text-align: center;">26</td><td style="text-align: center;">CORS</td><td>Cross-Origin Resource Sharing</td><td style="text-align: center;">Ch. 7</td></tr>
            <tr><td style="text-align: center;">27</td><td style="text-align: center;">RBAC</td><td>Role-Based Access Control</td><td style="text-align: center;">Ch. 7</td></tr>
            <tr><td style="text-align: center;">28</td><td style="text-align: center;">OWASP</td><td>Open Worldwide Application Security Project</td><td style="text-align: center;">References</td></tr>
        </tbody>
    </table>
</div>

<!-- ==================== LIST OF FIGURES ==================== -->
<div class="page-break">
    <div style="text-align: right; font-size: 10pt; font-style: italic;">viii</div>
    <h2 class="chap-header" style="margin-top: 20px; border: none;">LIST OF FIGURES</h2>
    <div style="height: 2px; background-color: #0d3b66; width: 60px; margin: 0 auto 20px auto;"></div>

    <table class="report-table">
        <thead>
            <tr>
                <th style="width: 10%;">S. No.</th>
                <th style="width: 20%;">Figure No.</th>
                <th style="width: 55%; text-align: left;">Title</th>
                <th style="width: 15%;">Page No.</th>
            </tr>
        </thead>
        <tbody>
            <tr><td style="text-align: center;">1</td><td style="text-align: center;">Figure 2.1</td><td>Use Case Diagram of SecureFileShare Platform</td><td style="text-align: center;">22</td></tr>
            <tr><td style="text-align: center;">2</td><td style="text-align: center;">Figure 2.2</td><td>Sequence Diagram of Recipient Ciphertext Upload and Key Sharing Pipeline</td><td style="text-align: center;">26</td></tr>
            <tr><td style="text-align: center;">3</td><td style="text-align: center;">Figure 2.3</td><td>Activity Diagram: Encrypted File Storage and Retrieval Flow</td><td style="text-align: center;">28</td></tr>
            <tr><td style="text-align: center;">4</td><td style="text-align: center;">Figure 3.1</td><td>Client-Service Monorepo Architecture of SecureFileShare</td><td style="text-align: center;">32</td></tr>
            <tr><td style="text-align: center;">5</td><td style="text-align: center;">Figure 3.2</td><td>Detailed Microservice Component &amp; Storage Topology</td><td style="text-align: center;">34</td></tr>
            <tr><td style="text-align: center;">6</td><td style="text-align: center;">Figure 3.3</td><td>Database Entity Relationship (ER) Diagram</td><td style="text-align: center;">37</td></tr>
            <tr><td style="text-align: center;">7</td><td style="text-align: center;">Figure 4.1</td><td>Continuous Integration and Continuous Deployment (CI/CD) Pipeline</td><td style="text-align: center;">70</td></tr>
        </tbody>
    </table>
</div>

<!-- ==================== LIST OF TABLES ==================== -->
<div class="page-break">
    <div style="text-align: right; font-size: 10pt; font-style: italic;">ix</div>
    <h2 class="chap-header" style="margin-top: 20px; border: none;">LIST OF TABLES</h2>
    <div style="height: 2px; background-color: #0d3b66; width: 60px; margin: 0 auto 20px auto;"></div>

    <table class="report-table">
        <thead>
            <tr>
                <th style="width: 10%;">S. No.</th>
                <th style="width: 20%;">Table No.</th>
                <th style="width: 55%; text-align: left;">Title</th>
                <th style="width: 15%;">Page No.</th>
            </tr>
        </thead>
        <tbody>
            <tr><td style="text-align: center;">1</td><td style="text-align: center;">Table 2.1</td><td>Functional Requirements of SecureFileShare</td><td style="text-align: center;">20</td></tr>
            <tr><td style="text-align: center;">2</td><td style="text-align: center;">Table 2.2</td><td>Non-Functional Requirements of SecureFileShare</td><td style="text-align: center;">21</td></tr>
            <tr><td style="text-align: center;">3</td><td style="text-align: center;">Table 3.1</td><td>Communication Flow Between System Layers of SecureFileShare</td><td style="text-align: center;">33</td></tr>
            <tr><td style="text-align: center;">4</td><td style="text-align: center;">Table 3.2</td><td>Entities and Attributes of SecureFileShare System</td><td style="text-align: center;">36</td></tr>
            <tr><td style="text-align: center;">5</td><td style="text-align: center;">Table 3.3</td><td>Database Tables and Structure Specifications</td><td style="text-align: center;">38</td></tr>
            <tr><td style="text-align: center;">6</td><td style="text-align: center;">Table 3.4</td><td>User Interface Components and Description</td><td style="text-align: center;">46</td></tr>
            <tr><td style="text-align: center;">7</td><td style="text-align: center;">Table 4.1</td><td>Work Breakdown Structure (WBS)</td><td style="text-align: center;">58</td></tr>
            <tr><td style="text-align: center;">8</td><td style="text-align: center;">Table 4.2</td><td>Project Timeline (8-Week Lifecycle)</td><td style="text-align: center;">59</td></tr>
            <tr><td style="text-align: center;">9</td><td style="text-align: center;">Table 4.3</td><td>Project Milestones &amp; Target Deliverables</td><td style="text-align: center;">61</td></tr>
            <tr><td style="text-align: center;">10</td><td style="text-align: center;">Table 4.4</td><td>Phase-wise Project Deliverables</td><td style="text-align: center;">62</td></tr>
            <tr><td style="text-align: center;">11</td><td style="text-align: center;">Table 4.5</td><td>Risk Analysis &amp; Impact Matrix</td><td style="text-align: center;">64</td></tr>
            <tr><td style="text-align: center;">12</td><td style="text-align: center;">Table 4.6</td><td>Cost Estimation of the Project</td><td style="text-align: center;">67</td></tr>
            <tr><td style="text-align: center;">13</td><td style="text-align: center;">Table 6.1</td><td>Sample Encrypted Vault File Records (Table View)</td><td style="text-align: center;">80</td></tr>
            <tr><td style="text-align: center;">14</td><td style="text-align: center;">Table 6.2</td><td>Sample Audit Trail &amp; Hash-Chain Verification Log</td><td style="text-align: center;">82</td></tr>
            <tr><td style="text-align: center;">15</td><td style="text-align: center;">Table 7.1</td><td>Unit Testing Details &amp; Validation Results</td><td style="text-align: center;">84</td></tr>
            <tr><td style="text-align: center;">16</td><td style="text-align: center;">Table 7.2</td><td>System Test Cases &amp; Security Validation Results</td><td style="text-align: center;">87</td></tr>
            <tr><td style="text-align: center;">17</td><td style="text-align: center;">Table 7.3</td><td>User Roles and Access Permissions Matrix</td><td style="text-align: center;">89</td></tr>
        </tbody>
    </table>
</div>
"""

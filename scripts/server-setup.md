<!--
================================================================================
File: scripts/server-setup.md
WHAT: Runbook for provisioning an Oracle Cloud ARM (Ampere A1) Ubuntu VM for SecureFileShare.
WHY: Sets up an isolated deployment user, Docker group access, firewall rules (OCI & OS),
     and secure directories while preserving any existing Docker workloads on the server.
================================================================================
-->

# SecureFileShare - Server Setup Runbook (Oracle Cloud ARM)

This guide documents the step-by-step procedure for preparing a fresh or existing Ubuntu 22.04 / 24.04 LTS ARM64 (Ampere A1) VM instance on Oracle Cloud Infrastructure (OCI).

Follow these numbered steps sequentially on the target server.

---

## 1. System Package Update and Core Utilities

Connect to your OCI VM as the default `ubuntu` user and update all system packages to eliminate known OS vulnerabilities.

```bash
# Update APT index and upgrade packages
sudo apt-get update && sudo apt-get upgrade -y

# Install essential diagnostic and networking tools
sudo apt-get install -y curl wget git jq ca-certificates gnupg lsb-release netfilter-persistent iptables-persistent
```

---

## 2. Create Dedicated Deployment User (`sfsdeploy`)

Create an isolated system user dedicated to executing GitHub Actions CD deployments.

*Why:* Avoids running automated deployments under the default `ubuntu` administrative account or the `root` superuser, enforcing the principle of least privilege.

```bash
# Create sfsdeploy user with a disabled password and dedicated home directory
sudo adduser --disabled-password --gecos "SecureFileShare CD Deployer" sfsdeploy

# Grant limited sudo rights if needed for directory management (optional, passwordless)
sudo bash -c 'cat <<EOF > /etc/sudoers.d/99-sfsdeploy
sfsdeploy ALL=(ALL) NOPASSWD: /bin/mkdir, /bin/chown, /bin/chmod, /usr/bin/docker
EOF'
sudo chmod 440 /etc/sudoers.d/99-sfsdeploy
```

---

## 3. Configure SSH Public Key for `sfsdeploy`

Configure authorized SSH keys so GitHub Actions can authenticate non-interactively using an ed25519 or RSA key pair.

*Why:* Key-based authentication prevents password brute-forcing and enables automated SSH deployment from GitHub Actions.

```bash
# Switch to sfsdeploy context and establish .ssh directory with strict permissions
sudo -u sfsdeploy mkdir -p /home/sfsdeploy/.ssh
sudo chmod 700 /home/sfsdeploy/.ssh

# Paste your deployment public key (matching the private key stored in GitHub Secrets)
# Replace the placeholder with your actual id_ed25519.pub content
sudo bash -c 'cat << "EOF" > /home/sfsdeploy/.ssh/authorized_keys
ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAI... deploy-key@securefileshare
EOF'

# Enforce secure ownership and permissions
sudo chown -R sfsdeploy:sfsdeploy /home/sfsdeploy/.ssh
sudo chmod 600 /home/sfsdeploy/.ssh/authorized_keys
```

---

## 4. Install Docker Engine and Docker Compose V2

Install the official Docker Engine and Docker Compose CLI plugin from Docker's upstream repository (not the distribution fork).

*Why:* Ensures support for modern Docker Compose V2 syntax, multi-architecture ARM64 builds, and cgroup v2 resource limits.

```bash
# Add Docker official GPG key
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

# Add Docker upstream repository to APT sources
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker Engine, CLI, Containerd, and Compose plugin
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Enable and start Docker service
sudo systemctl enable --now docker
```

---

## 5. Add Deploy User to Docker Group

Allow `sfsdeploy` to communicate with the Docker daemon UNIX socket `/var/run/docker.sock` without requiring `sudo`.

```bash
# Add sfsdeploy to docker group
sudo usermod -aG docker sfsdeploy

# Verify membership
id sfsdeploy
```

---

## 6. Create Dedicated Project Directory (`/opt/securefileshare`)

Create the application directory where Compose manifests, Caddyfile, and environment secrets reside.

*Why:* Placing the stack inside `/opt/securefileshare` with `sfsdeploy:sfsdeploy` ownership ensures it remains completely segregated from any other containerized applications running on the VM.

```bash
# Create dedicated directory hierarchy
sudo mkdir -p /opt/securefileshare/docker/postgres

# Assign ownership to sfsdeploy
sudo chown -R sfsdeploy:sfsdeploy /opt/securefileshare

# Restrict folder access to sfsdeploy and root
sudo chmod 750 /opt/securefileshare
```

---

## 7. Configure OCI Virtual Cloud Network (VCN) Ingress Rules

In the Oracle Cloud Console, navigate to **Networking > Virtual Cloud Networks > [Your VCN] > Security Lists > Default Security List** (or your Network Security Group) and configure the following Ingress Rules:

| Stateless | Source CIDR | IP Protocol | Source Port | Destination Port | Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **No** | `0.0.0.0/0` | TCP | All | `80` | Public HTTP for ACME Let's Encrypt validation |
| **No** | `0.0.0.0/0` | TCP | All | `8443` | Public HTTPS for SecureFileShare (preserves port 443 for Wazuh) |
| **No** | `YOUR_ADMIN_IP/32` | TCP | All | `22` | Restricted SSH management (do NOT leave open to 0.0.0.0/0) |

*Why:* Restricting port 22 to your personal IP or VPN CIDR prevents automated SSH brute-force bots, while port 80 (HTTP) and port 8443 (HTTPS) must be accessible from the internet for Caddy TLS certificate issuance and client access without clashing with Wazuh on 443.

---

## 8. Configure Ubuntu Host OS Firewall (OCI iptables Rules)

*Crucial OCI Notice:* Default Ubuntu images in Oracle Cloud contain restrictive `iptables` rules that drop incoming traffic on ports 80 and 443 even when allowed in the OCI Console Security List.

Execute the following commands to permit ports 80 and 443 in the host OS kernel netfilter tables:

```bash
# Allow inbound HTTP (80) and HTTPS (443) before the final REJECT rule in INPUT chain
sudo iptables -I INPUT 6 -p tcp --dport 80 -m conntrack --ctstate NEW,ESTABLISHED -j ACCEPT
sudo iptables -I INPUT 6 -p tcp --dport 443 -m conntrack --ctstate NEW,ESTABLISHED -j ACCEPT

# Persist iptables rules across system reboots
sudo netfilter-persistent save
sudo netfilter-persistent reload
```

If using `ufw` instead of raw `iptables`:
```bash
sudo ufw allow 80/tcp comment "HTTP for Caddy ACME"
sudo ufw allow 443/tcp comment "HTTPS for SecureFileShare"
sudo ufw allow from YOUR_ADMIN_IP to any port 22 proto tcp comment "Admin SSH"
sudo ufw enable
```

---

## 9. Populate GitHub Actions Repository Secrets

In your GitHub repository, navigate to **Settings > Secrets and variables > Actions** and add the following repository secrets:

| Secret Name | Example / Purpose |
| :--- | :--- |
| `SERVER_HOST` | `129.153.x.x` (Public IP of your Oracle Cloud VM) |
| `SERVER_USER` | `sfsdeploy` (Dedicated deploy user created in Step 2) |
| `SERVER_PORT` | `22` (Or your custom SSH port) |
| `SERVER_SSH_KEY` | Contents of your private SSH key (`id_ed25519` private key) |
| `SERVER_DOMAIN` | `sfs.yourdomain.com` (FQDN pointing to your VM IP) |
| `POSTGRES_USER` | `sfs_prod_admin` |
| `POSTGRES_PASSWORD` | Cryptographically random secret (`openssl rand -base64 32`) |
| `REDIS_PASSWORD` | Cryptographically random secret (`openssl rand -base64 32`) |
| `MINIO_ROOT_USER` | `sfs_minio_admin` |
| `MINIO_ROOT_PASSWORD` | Cryptographically random secret (`openssl rand -base64 32`) |
| `JWT_SECRET` | 64-character hex secret (`openssl rand -hex 32`) |
| `JWT_EXPIRATION_MS` | `900000` (15 minutes in milliseconds) |

---

## 10. Deployment Verification Procedures

Once GitHub Actions finishes deploying, execute these verification commands to confirm system integrity:

### 10.1 Verify Container Status
Run from your local workstation or while logged in as `sfsdeploy`:
```bash
ssh sfsdeploy@YOUR_SERVER_IP "cd /opt/securefileshare && docker compose -f docker-compose.prod.yml ps"
```
*Expected Output:* All containers (`sfs_prod_caddy`, `sfs_prod_postgres`, `sfs_prod_redis`, `sfs_prod_minio`, `sfs_prod_auth`, `sfs_prod_file`, `sfs_prod_share`, `sfs_prod_audit`) show `Up` and `(healthy)`.

### 10.2 Verify Zero Exposed Datastore Ports
Check open listening ports on the host VM:
```bash
ssh sfsdeploy@YOUR_SERVER_IP "ss -tulpn | grep -E ':(5432|6379|9000|9001|8081|8082|8083|8084)'"
```
*Expected Output:* Empty! None of the datastore or Spring microservice ports should appear in the host listening table. Only ports 80, 8443 (bound by `docker-proxy` for Caddy), 443 (Wazuh), and port 22 (SSH) must be visible.

### 10.3 Verify HTTPS and Health Endpoints
From any external computer:
```bash
# Check Caddy public gateway health
curl -i https://sfs.yourdomain.com:8443/health

# Check auth-service health through Caddy reverse proxy
curl -i https://sfs.yourdomain.com:8443/api/v1/auth/health
```
*Expected Output:* HTTP `200 OK` with JSON response:
```json
{"status":"UP","service":"auth-service","timestamp":"2026-10-30T..."}
```

### 10.4 Verify Security Response Headers
```bash
curl -I https://sfs.yourdomain.com:8443/api/v1/auth/health
```
*Expected Output:* Response includes:
- `Strict-Transport-Security: max-age=31536000; includeSubDomains; preload`
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'; base-uri 'none';`
- Absence of `Server: ...` banner header.

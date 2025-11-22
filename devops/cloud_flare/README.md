# 🔒 Cloudflare Tunnel Setup Guide for Services Behind NAT

## 📖 Overview
This guide provides step-by-step instructions for setting up Cloudflare Tunnel to securely expose services running behind NAT with port restrictions (common with ISPs like Vodafone). The tunnel creates a secure outbound connection from your server to Cloudflare, bypassing the need for port forwarding.

## 🎯 Use Case
- Expose web services from behind restrictive ISP firewalls
- Secure access to home lab/services without opening ports
- Bypass ISP port blocking (80/443 commonly blocked)
- Zero-trust network access implementation

---

## 📋 Prerequisites

### Required
- Domain name managed by Cloudflare (`example.com`)
- Cloudflare account with the domain added
- Server with internet access (Ubuntu/Debian)
- Local service running on the server (port 8000 in this example)

### Verified Environment
- **Server**: Ubuntu 22.04 LTS
- **Domain**: tlab29.com
- **Local Service**: Traefik on port 8000
- **ISP**: Vodafone (ports 80/443 blocked)

---

## 🔧 Step-by-Step Setup

### 1. Install cloudflared

```bash
# Download and install cloudflared
wget -q https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64
sudo mv cloudflared-linux-amd64 /usr/local/bin/cloudflared
sudo chmod +x /usr/local/bin/cloudflared

# Verify installation
cloudflared --version
```

**Expected Output**: `cloudflared version 2024.8.0 (built 2024-08-20-1234)`

### 2. Authenticate with Cloudflare

```bash
cloudflared tunnel login
```

This command will:
- Open a browser window for authentication
- Request permission to manage your domain
- Generate authentication certificate at `~/.cloudflared/cert.pem`

### 3. Create Tunnel

```bash
# Create a new tunnel
cloudflared tunnel create tlab29-tunnel

# Expected output:
# Tunnel credentials written to /home/user/.cloudflared/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.json
# Tunnel created with ID: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

**⚠️ Important**: Save the Tunnel ID displayed in the output.

### 4. Configure Tunnel

```bash
# Create configuration directory
sudo mkdir -p /etc/cloudflared

# Create config file (REPLACE WITH YOUR TUNNEL ID)
sudo tee /etc/cloudflared/config.yml > /dev/null <<EOF
        tunnel: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
        credentials-file: /etc/cloudflared/credentials.json

ingress:
- hostname: tlab29.com
service: http://localhost:8000
- hostname: www.tlab29.com
service: http://localhost:8000
- hostname: api.tlab29.com
service: http://localhost:8000
- service: http_status:404
EOF

# Copy credentials file (REPLACE WITH YOUR TUNNEL ID)
sudo cp /home/$(whoami)/.cloudflared/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.json /etc/cloudflared/credentials.json
```

### 5. Install as System Service

```bash
# Install cloudflared as a system service
sudo cloudflared service install

# Start and enable the service
sudo systemctl start cloudflared
sudo systemctl enable cloudflared

# Verify service status
sudo systemctl status cloudflared
```

**Expected Status**: `Active (running)`

### 6. Configure Cloudflare DNS

In Cloudflare Dashboard → **DNS**:

1. **Remove** existing A records for your domain
2. **Add** CNAME records:

| Type | Name | Target | Proxy Status |
|------|------|--------|-------------|
| CNAME | tlab29.com | `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.cfargotunnel.com` | Proxied (orange cloud) |
| CNAME | www | `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.cfargotunnel.com` | Proxied |
| CNAME | api | `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.cfargotunnel.com` | Proxied |

### 7. (Optional) Configure Routes Explicitly

```bash
# Explicitly route domains to tunnel
cloudflared tunnel route dns xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx tlab29.com
cloudflared tunnel route dns xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx www.tlab29.com
cloudflared tunnel route dns xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx api.tlab29.com
```

---

## 🧪 Verification Tests

### Test 1: Tunnel Status

```bash
# List all tunnels
cloudflared tunnel list

# Expected output:
# ID                                   NAME          CREATED              CONNECTIONS
# xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx tlab29-tunnel 2025-11-21T14:38:43Z fra08,fra10...
```

### Test 2: Service Status

```bash
# Check cloudflared service
sudo systemctl status cloudflared

# View real-time logs
sudo journalctl -u cloudflared -f
```

### Test 3: DNS Resolution

```bash
# Verify DNS points to Cloudflare, not your server IP
nslookup tlab29.com

# Expected: Returns Cloudflare IPs (104.* or 172.*), not your server IP
```

### Test 4: Local Service Check

```bash
# Verify local service is accessible
curl -H "Host: tlab29.com" http://localhost:8000/
```

### Test 5: End-to-End Test

```bash
# Test through Cloudflare
curl -L https://tlab29.com/
curl -L https://api.tlab29.com/actuator/health
```

---

## 🔄 Management Commands

### Restart Tunnel
```bash
sudo systemctl restart cloudflared
```

### View Logs
```bash
# Last 50 lines
sudo journalctl -u cloudflared -n 50

# Follow logs in real-time
sudo journalctl -u cloudflared -f
```

### Update Configuration
```bash
# Edit config file
sudo nano /etc/cloudflared/config.yml

# Restart after changes
sudo systemctl restart cloudflared
```

### Delete Tunnel
```bash
# Stop and disable service
sudo systemctl stop cloudflared
sudo systemctl disable cloudflared

# Delete tunnel
cloudflared tunnel delete tlab29-tunnel

# Remove configuration
sudo rm -rf /etc/cloudflared
```

---

## 🚨 Troubleshooting

### Error: "Direct IP access not allowed" (1003)
**Cause**: DNS records are A records or not proxied
**Solution**:
- Ensure all records are CNAME, not A records
- Verify Proxy status is "Proxied" (orange cloud)
- Check tunnel is connected: `cloudflared tunnel list`

### Error: "502 Bad Gateway"
**Cause**: Local service not responding
**Solution**:
```bash
# Test local service
curl -H "Host: tlab29.com" http://localhost:8000/

# Check service logs
sudo journalctl -u cloudflared | grep -i error
```

### Error: Tunnel Not Connecting
**Solution**:
```bash
# Reinstall service
sudo cloudflared service uninstall
sudo cloudflared service install
sudo systemctl start cloudflared

# Check connectivity
cloudflared tunnel info tlab29-tunnel
```

### DNS Not Propagating
**Solution**:
- Wait 5-30 minutes for DNS propagation
- Test from different network (mobile data)
- Clear local DNS cache: `sudo systemd-resolve --flush-caches`

---

## 🔒 Security Considerations

- ✅ **No open ports** required on router
- ✅ **Encrypted tunnel** between server and Cloudflare
- ✅ **DDoS protection** by Cloudflare
- ✅ **Web Application Firewall** (WAF) available
- ✅ **Zero-trust** security model

---

## 📈 Monitoring

### Cloudflare Analytics
- **Dashboard**: Traffic analytics and security events
- **Logs**: HTTP request logs
- **Analytics**: Performance metrics

### Server Monitoring
```bash
# Monitor tunnel connections
watch -n 5 'cloudflared tunnel list'

# Service health
sudo systemctl status cloudflared

# Resource usage
top -p $(pgrep cloudflared)
```

---

## 🎯 Next Steps: HTTPS Configuration

Once the tunnel is working with HTTP, proceed with HTTPS setup:

1. **Cloudflare SSL Settings**: SSL/TLS → Overview → Full
2. **Enable Always Use HTTPS**: SSL/TLS → Edge Certificates
3. **Update Tunnel for HTTPS**: Change service to `https://localhost:8443`
4. **Configure Traefik SSL**: Set up Let's Encrypt or Cloudflare Origin Certificate
5. **Browser Testing**: Verify green lock icon

---

## 💡 Additional Resources

- [Cloudflare Tunnel Documentation](https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/)
- [Tunnel Configuration Reference](https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/install-and-setup/tunnel-guide/)
- [Troubleshooting Guide](https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/troubleshooting/)

---

*Documentation based on successful implementation for tlab29.com*
*Last Updated: 2025-11-21 | Server: gateway-core (Ubuntu 22.04)*
*ISP: Vodafone | Ports Blocked: 80, 443, 8000-8443*
# Deployment Guide - SSL Certificate Installation

## Where to Run SSL Installation

### YES - Run on YOUR SERVER!

The command `sudo ./install-ssl.sh yourdomain.com admin@yourdomain.com` must be executed on your **production server** where:
- Your domain points to (DNS A record)
- Your Docker containers run
- Port 80 and 443 are accessible

## Step-by-Step Deployment

### 1. Prepare Server
```bash
# SSH to your server
ssh root@your-server-ip

# Navigate to project directory
cd /path/to/DeliveryOfRolls

# Make script executable
chmod +x install-ssl.sh
```

### 2. Install SSL Certificate
```bash
# Replace with YOUR actual domain and email
sudo ./install-ssl.sh yourdomain.com admin@yourdomain.com

# Example with real domain:
sudo ./install-ssl.sh deliveryofrolls.ru admin@deliveryofrolls.ru
```

### 3. Update Configuration Files
```bash
# Edit docker-compose.yml
nano docker-compose.yml

# Change ports from 8080:8080 to:
ports:
  - "8443:8443"  # HTTPS
  - "/etc/letsencrypt:/etc/letsencrypt:ro"  # Add SSL volume
```

### 4. Update Application Properties
```bash
# Already done in application-prod.properties:
server.port=8443
spring.datasource.url=jdbc:mysql://db:3306/sushishop?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true
```

### 5. Deploy Application
```bash
# Stop current containers
docker-compose down

# Build new image (if needed)
docker build -t redrolls-app:latest ./deploy

# Start with HTTPS
docker-compose up -d

# Check logs
docker-compose logs -f app
```

## Prerequisites

### Server Requirements:
- **Ubuntu/Debian** Linux
- **Root access** (sudo)
- **Domain pointing** to server IP
- **Ports 80 and 443** open
- **Docker and Docker Compose** installed

### DNS Setup:
```
A Record: yourdomain.com -> YOUR_SERVER_IP
A Record: www.yourdomain.com -> YOUR_SERVER_IP
```

### Firewall Rules:
```bash
# Allow HTTP/HTTPS
ufw allow 80
ufw allow 443

# Allow SSH (if not already)
ufw allow 22
```

## Verification

### Check Certificate:
```bash
# Check certificate files
ls -la /etc/letsencrypt/live/yourdomain.com/

# Test renewal
sudo certbot renew --dry-run
```

### Test HTTPS:
```bash
# Test HTTPS connection
curl -I https://yourdomain.com

# Check certificate details
openssl s_client -connect yourdomain.com:443 -servername yourdomain.com
```

## Troubleshooting

### Certificate Not Found:
```bash
# Check certbot logs
sudo journalctl -u certbot

# Manual certificate request
sudo certbot certonly --standalone -d yourdomain.com
```

### Port Conflicts:
```bash
# Stop services using port 80/443
sudo systemctl stop nginx
sudo systemctl stop apache2

# Check what's using ports
sudo netstat -tulpn | grep :80
sudo netstat -tulpn | grep :443
```

### Docker Issues:
```bash
# Check Docker logs
docker-compose logs app

# Restart containers
docker-compose restart app
```

## Auto-Renewal

The script automatically sets up cron job for certificate renewal:
```bash
# Check cron job
crontab -l

# Manual renewal test
sudo certbot renew --dry-run
```

## Security Tips

1. **Backup certificates:**
   ```bash
   tar -czf letsencrypt-backup.tar.gz /etc/letsencrypt/
   ```

2. **Monitor renewal:**
   ```bash
   # Add to crontab for monitoring
   0 6 * * * certbot renew --quiet && docker-compose restart nginx
   ```

3. **Use strong passwords:**
   - Update default passwords
   - Use environment variables for secrets

## Production Checklist

- [ ] Domain DNS configured
- [ ] Firewall ports 80/443 open
- [ ] Docker containers running
- [ ] SSL certificate installed
- [ ] HTTPS working in browser
- [ ] Auto-renewal configured
- [ ] Backup strategy in place

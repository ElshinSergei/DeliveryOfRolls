# SSL Certificate Setup with Let's Encrypt

## 1. Installation (Ubuntu/Debian)

```bash
# Update package list
sudo apt update

# Install Certbot
sudo apt install certbot python3-certbot-nginx

# Or for Apache
sudo apt install certbot python3-certbot-apache
```

## 2. Configuration

### Option A: Nginx (Recommended)
```bash
# Generate certificate
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com

# Auto-renewal
sudo crontab -e
# Add: 0 12 * * * /usr/bin/certbot renew --quiet
```

### Option B: Standalone (if no web server)
```bash
# Stop your application
sudo systemctl stop your-app

# Generate certificate
sudo certbot certonly --standalone -d yourdomain.com

# Start your application
sudo systemctl start your-app
```

## 3. Spring Boot Configuration

Add to `application.properties`:
```properties
# HTTPS Configuration
server.ssl.enabled=true
server.ssl.key-store=/etc/letsencrypt/live/yourdomain.com/keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=tomcat

# HTTP to HTTPS redirect
server.http.port=80
```

## 4. Convert Let's Encrypt to PKCS12

```bash
# Convert certificates
sudo openssl pkcs12 -export \
  -in /etc/letsencrypt/live/yourdomain.com/fullchain.pem \
  -inkey /etc/letsencrypt/live/yourdomain.com/privkey.pem \
  -out /etc/letsencrypt/live/yourdomain.com/keystore.p12 \
  -name tomcat \
  -CAfile /etc/letsencrypt/live/yourdomain.com/chain.pem \
  -caname root

# Set permissions
sudo chown $USER:$USER /etc/letsencrypt/live/yourdomain.com/keystore.p12
```

## 5. Test Certificate

```bash
# Test renewal
sudo certbot renew --dry-run

# Check certificate
openssl x509 -in /etc/letsencrypt/live/yourdomain.com/cert.pem -text -noout
```

## 6. Docker Alternative

```dockerfile
FROM certbot/certbot:latest
CMD certbot certonly --standalone -d yourdomain.com --email admin@yourdomain.com --agree-tos --non-interactive
```

## 7. Nginx Reverse Proxy (Alternative)

```nginx
server {
    listen 80;
    server_name yourdomain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl;
    server_name yourdomain.com;
    
    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;
    
    location / {
        proxy_pass http://localhost:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

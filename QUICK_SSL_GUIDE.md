# Quick SSL Setup Guide for Delivery of Rolls

## Option 1: Development (Self-Signed Certificate)

```bash
# Generate self-signed certificate
keytool -genkeypair -alias delivery -storetype PKCS12 -keystore delivery.p12 -validity 365

# Move to resources folder
mv delivery.p12 src/main/resources/

# Update application.properties
server.ssl.enabled=true
server.ssl.key-store=classpath:delivery.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=delivery
```

## Option 2: Production (Let's Encrypt)

### Step 1: Install SSL
```bash
chmod +x install-ssl.sh
sudo ./install-ssl.sh yourdomain.com admin@yourdomain.com
```

### Step 2: Update application.properties
```properties
# Uncomment these lines in application.properties
server.ssl.enabled=true
server.ssl.key-store=/etc/letsencrypt/live/yourdomain.com/keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=tomcat
```

### Step 3: Restart Application
```bash
# Stop your application
./mvnw spring-boot:stop

# Start with HTTPS
./mvnw spring-boot:run
```

## Testing

```bash
# Test HTTPS
curl -I https://yourdomain.com

# Check certificate
openssl s_client -connect yourdomain.com:443 -servername yourdomain.com
```

## Troubleshooting

### Certificate Not Found
```bash
# Check certificate location
ls -la /etc/letsencrypt/live/yourdomain.com/

# Check permissions
sudo chown $USER:$USER /etc/letsencrypt/live/yourdomain.com/keystore.p12
```

### Port Already in Use
```bash
# Check what's using port 443
sudo netstat -tulpn | grep :443

# Stop conflicting service
sudo systemctl stop nginx
```

### Auto-Renewal
```bash
# Test renewal
sudo certbot renew --dry-run

# Check cron job
crontab -l
```

## Security Headers (Add to application.properties)

```properties
# Security Headers
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=strict
security.require-ssl=true
```

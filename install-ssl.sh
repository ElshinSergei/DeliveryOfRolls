#!/bin/bash

# SSL Certificate Installation Script for Delivery of Rolls
# Usage: ./install-ssl.sh yourdomain.com

DOMAIN=${1:-"localhost"}
EMAIL=${2:-"admin@yourdomain.com"}

echo "=== SSL Certificate Installation for $DOMAIN ==="

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    echo "Please run as root (use sudo)"
    exit 1
fi

# Update package list
echo "Updating package list..."
apt update

# Install Certbot and Nginx
echo "Installing Certbot and Nginx..."
apt install -y certbot python3-certbot-nginx nginx

# Stop Nginx if running
echo "Stopping Nginx..."
systemctl stop nginx

# Generate certificate (standalone mode)
echo "Generating SSL certificate for $DOMAIN..."
certbot certonly --standalone -d $DOMAIN --email $EMAIL --agree-tos --non-interactive

# Convert to PKCS12 format
echo "Converting certificate to PKCS12..."
CERT_PATH="/etc/letsencrypt/live/$DOMAIN"
openssl pkcs12 -export \
    -in $CERT_PATH/fullchain.pem \
    -inkey $CERT_PATH/privkey.pem \
    -out $CERT_PATH/keystore.p12 \
    -name tomcat \
    -CAfile $CERT_PATH/chain.pem \
    -caname root \
    -password pass:changeit

# Set permissions
chown $USER:$USER $CERT_PATH/keystore.p12

# Create Nginx configuration
echo "Creating Nginx configuration..."
cat > /etc/nginx/sites-available/deliveryofrolls << EOF
server {
    listen 80;
    server_name $DOMAIN;
    return 301 https://\$server_name\$request_uri;
}

server {
    listen 443 ssl;
    server_name $DOMAIN;
    
    ssl_certificate $CERT_PATH/fullchain.pem;
    ssl_certificate_key $CERT_PATH/privkey.pem;
    
    location / {
        proxy_pass http://localhost:8081;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF

# Enable site
ln -sf /etc/nginx/sites-available/deliveryofrolls /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default

# Test Nginx configuration
echo "Testing Nginx configuration..."
nginx -t

# Start Nginx
echo "Starting Nginx..."
systemctl start nginx

# Setup auto-renewal
echo "Setting up auto-renewal..."
(crontab -l 2>/dev/null; echo "0 12 * * * /usr/bin/certbot renew --quiet && systemctl reload nginx") | crontab -

# Test certificate renewal
echo "Testing certificate renewal..."
certbot renew --dry-run

echo "=== SSL Certificate Installation Complete ==="
echo "Certificate location: /etc/letsencrypt/live/$DOMAIN/"
echo "Keystore location: /etc/letsencrypt/live/$DOMAIN/keystore.p12"
echo "Your application should now be available at: https://$DOMAIN"
echo ""
echo "Next steps:"
echo "1. Update application.properties with your domain"
echo "2. Uncomment SSL configuration"
echo "3. Restart your application"
echo "4. Test HTTPS access"

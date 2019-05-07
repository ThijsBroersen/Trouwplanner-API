#!/usr/bin/env bash
docker pull nlxio/outway:latest
docker run --detach \
     --name my-nlx-outway \
     --volume ~/nlx-setup/root.crt:/certs/root.crt:ro \
     --volume ~/nlx-setup/org.crt:/certs/org.crt:ro \
     --volume ~/nlx-setup/org.key:/certs/org.key:ro \
     --env DIRECTORY_INSPECTION_ADDRESS=directory-inspection-api.demo.nlx.io:443 \
     --env TLS_NLX_ROOT_CERT=/certs/root.crt \
     --env TLS_ORG_CERT=/certs/org.crt \
     --env TLS_ORG_KEY=/certs/org.key \
     --env DISABLE_LOGDB=1 \
     --publish 4080:80 \
     --restart unless-stopped \
     nlxio/outway:latest

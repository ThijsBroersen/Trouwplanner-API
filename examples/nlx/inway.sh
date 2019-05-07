#!/usr/bin/env bash
docker pull nlxio/inway:latest
docker run --detach \
    --name my-nlx-inway \
    --volume ~/nlx-setup/root.crt:/certs/root.crt:ro \
    --volume ~/nlx-setup/org.crt:/certs/org.crt:ro \
    --volume ~/nlx-setup/org.key:/certs/org.key:ro \
    --volume $(pwd)/service-config.toml:/service-config.toml:ro \
    --env DIRECTORY_REGISTRATION_ADDRESS=directory-registration-api.demo.nlx.io:443 \
    --env SELF_ADDRESS=convenantgemeenten.nl:4443 \
    --env SERVICE_CONFIG=/service-config.toml \
    --env TLS_NLX_ROOT_CERT=/certs/root.crt \
    --env TLS_ORG_CERT=/certs/org.crt \
    --env TLS_ORG_KEY=/certs/org.key \
    --env DISABLE_LOGDB=1 \
    --publish 4443:443 \
    --restart unless-stopped \
    nlxio/inway:latest

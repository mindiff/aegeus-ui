## Build the AEG WebUI image

```
rm -rf docker
mkdir -p docker

export NVERSION=1.0.0.Alpha5-SNAPSHOT

tar xzf aegeus-dist-$NVERSION-deps.tgz -C docker
tar xzf aegeus-dist-$NVERSION-proj.tgz -C docker

cat << EOF > docker/Dockerfile
FROM nessusio/fedoraj:29

# Install the binaries
COPY aegeus-dist-$NVERSION aegeus-webui

# Make the entrypoint executable
RUN ln -s /aegeus-webui/bin/run-aegeus-webui.sh /usr/local/bin/aegeus-webui

ENTRYPOINT ["aegeus-webui"]
EOF

docker build -t aegeus/aegeus-webui docker/
docker push aegeus/aegeus-webui

export TAGNAME=1.0.0.Alpha5-dev
docker tag aegeus/aegeus-webui aegeus/aegeus-webui:$TAGNAME
docker push aegeus/aegeus-webui:$TAGNAME
```

### Run the WebUI image

```
export CNAME=aeg-webui
export LABEL=Bob

docker rm -f $CNAME
docker run --detach \
    -p 8082:8082 \
    --link aegd:blockchain \
    --link aeg-ipfs:ipfs \
    --link aeg-jaxrs:jaxrs \
    --env NESSUS_WEBUI_LABEL=$LABEL \
    --memory=100m --memory-swap=2g \
    --name $CNAME \
    aegeus/aegeus-webui
    
docker logs -f aeg-webui
```

### Run the WebUI in mixed mode

This assumes you have the Blockchain and IPFS instances already running on your host

```
export CNAME=aeg-webui
export LOCALIP=192.168.178.20
export LABEL=Mary

docker rm -f $CNAME
docker run --detach \
    -p 8082:8082 \
    --link aeg-jaxrs:jaxrs \
    --env IPFS_GATEWAY_ADDR=$LOCALIP \
    --env IPFS_GATEWAY_PORT=8080 \
    --env BLOCKCHAIN_JSONRPC_ADDR=$LOCALIP \
    --env BLOCKCHAIN_JSONRPC_PORT=51473 \
    --env BLOCKCHAIN_JSONRPC_USER=aeg \
    --env BLOCKCHAIN_JSONRPC_PASS=aegpass \
    --env NESSUS_WEBUI_LABEL=$LABEL \
    --memory=100m --memory-swap=2g \
    --name $CNAME \
    aegeus/aegeus-webui

docker logs -f webui
```
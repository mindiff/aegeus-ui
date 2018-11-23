## Build the AEG JSON-RPC bridge image

```
rm -rf docker
mkdir -p docker

export NVERSION=1.0.0.Alpha5-SNAPSHOT

tar xzf aegeus-dist-$NVERSION-deps.tgz -C docker
tar xzf aegeus-dist-$NVERSION-proj.tgz -C docker

cat << EOF > docker/Dockerfile
FROM nessusio/fedoraj:29

# Install the binaries
COPY aegeus-dist-$NVERSION aegeus-jaxrs

# Make the entrypoint executable
RUN ln -s /aegeus-jaxrs/bin/run-aegeus-jaxrs.sh /usr/local/bin/aegeus-jaxrs

# Expose the JAXRS port
EXPOSE 8081

ENTRYPOINT ["aegeus-jaxrs"]
EOF

docker build -t aegeus/aegeus-jaxrs docker/
docker push aegeus/aegeus-jaxrs

export TAGNAME=1.0.0.Alpha5-dev
docker tag aegeus/aegeus-jaxrs aegeus/aegeus-jaxrs:$TAGNAME
docker push aegeus/aegeus-jaxrs:$TAGNAME
```

### Run the JAXRS image

```
export CNAME=aeg-jaxrs

docker rm -f $CNAME
docker run --detach \
    --link aegd:blockchain \
    --link aeg-ipfs:ipfs \
    --memory=100m --memory-swap=2g \
    --name $CNAME \
    aegeus/aegeus-jaxrs

docker logs -f aeg-jaxrs
```

### Run the JAXRS in mixed mode

This assumes you have the Blockchain and IPFS instances already running on your host

```
export CNAME=aeg-jaxrs
export LOCALIP=192.168.178.20

docker rm -f $CNAME
docker run --detach \
    --env IPFS_JSONRPC_ADDR=$LOCALIP \
    --env IPFS_JSONRPC_PORT=5001 \
    --env BLOCKCHAIN_JSONRPC_ADDR=$LOCALIP \
    --env BLOCKCHAIN_JSONRPC_PORT=51473 \
    --env BLOCKCHAIN_JSONRPC_USER=aeg \
    --env BLOCKCHAIN_JSONRPC_PASS=aegpass \
    --memory=100m --memory-swap=2g \
    --name $CNAME \
    aegeus/aegeus-jaxrs
    
docker logs aeg-jaxrs
```
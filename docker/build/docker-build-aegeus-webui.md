## Build the AEG WebUI image

```
rm -rf docker
mkdir -p docker

export NVERSION=1.0.0.Alpha5-SNAPSHOT

tar xzf aegeus-dist-$NVERSION-deps.tgz -C docker
tar xzf aegeus-dist-$NVERSION-proj.tgz -C docker

cat << EOF > docker/Dockerfile
FROM nessusio/fedoraj

# Install the binaries
COPY aegeus-dist-$NVERSION aegeus-webui

# Make the entrypoint executable
RUN ln -s /aegeus-webui/bin/run-aegeus-webui.sh /usr/local/bin/aegeus-webui

ENTRYPOINT ["aegeus-webui"]
EOF

docker build -t aegeus/aegeus-webui docker/

export TAGNAME=1.0.0.Alpha5-dev
docker tag aegeus/aegeus-webui aegeus/aegeus-webui:$TAGNAME
docker push aegeus/aegeus-webui:$TAGNAME
docker push aegeus/aegeus-webui
```

Run the AEG WebUI

```
export NAME=webui
export LABEL=Bob

docker rm -f $NAME
docker run --detach \
    -p 8082:8082 \
    --link aegd:aeg \
    --link ipfs:ipfs \
    --link jaxrs:jaxrs \
    --env AEG_WEBUI_LABEL=$LABEL \
    --memory=200m --memory-swap=2g \
    --name $NAME \
    aegeus/aegeus-webui
    
watch docker logs webui
```

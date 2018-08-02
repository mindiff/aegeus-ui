## Build the AEG WebUI image

```
rm -rf docker
mkdir -p docker

export NVERSION=1.0.0.Alpha4-SNAPSHOT

tar xzf aegeus-dist-$NVERSION-deps.tgz -C docker
tar xzf aegeus-dist-$NVERSION-proj.tgz -C docker

cat << EOF > docker/Dockerfile
FROM aegeus/aegeus-ipfsj

# Install the binaries
COPY aegeus-dist-$NVERSION aegeus-webui

# Make the entrypoint executable
RUN ln -s /aegeus-webui/bin/run-aegeus-webui.sh /usr/local/bin/aegeus-webui

ENTRYPOINT ["aegeus-webui"]
EOF

docker rmi -f aegeus/aegeus-webui
docker build -t aegeus/aegeus-webui docker/

docker push aegeus/aegeus-webui

docker tag aegeus/aegeus-webui aegeus/aegeus-webui:$NVERSION
docker push aegeus/aegeus-webui:$NVERSION
```

Run the AEG WebUI

```
export LABEL=Bob
export NAME=aeg-webui

docker rm -f $NAME
docker run --detach \
    -p 8082:8082 \
    --link aegd:aeg \
    --link aeg-ipfs:ipfs \
    --link aeg-jaxrs:jaxrs \
    --env AEG_WEBUI_LABEL=$LABEL \
    --memory=200m --memory-swap=2g \
    --name $NAME \
    aegeus/aegeus-webui
    
watch docker logs $NAME

docker exec -it $NAME tail -f -n 100 debug.log
```

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

CMD ["start"]
ENTRYPOINT ["aegeus-jaxrs"]
EOF

docker build -t aegeus/aegeus-jaxrs docker/
docker push aegeus/aegeus-jaxrs

export TAGNAME=1.0.0.Alpha5-dev
docker tag aegeus/aegeus-jaxrs aegeus/aegeus-jaxrs:$TAGNAME
docker push aegeus/aegeus-jaxrs:$TAGNAME
```

### Run the AEG JAXRS image

```
export CNAME=aeg-jaxrs

docker rm -f $CNAME
docker run --detach \
    --link aegd:aeg \
    --link aeg-ipfs:ipfs \
    --expose 8081 \
    --memory=100m --memory-swap=2g \
    --name $CNAME \
    aegeus/aegeus-jaxrs

# Follow the info log
docker logs -f aeg-jaxrs

# Follow the info log on the journal
journalctl CONTAINER_NAME=aeg-jaxrs -f

# Follow the debug log
docker exec -it aeg-jaxrs tail -f -n 100 debug.log
```

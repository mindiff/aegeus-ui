## Build the AEG JSON-RPC bridge image

```
rm -rf docker
mkdir -p docker

export NVERSION=1.0.0.Alpha3

tar xzf aegeus-dist-$NVERSION-deps.tgz -C docker
tar xzf aegeus-dist-$NVERSION-proj.tgz -C docker

cat << EOF > docker/Dockerfile
FROM aegeus/aegeus-ipfsj

# Install the binaries
COPY aegeus-dist-$NVERSION aegeus-jaxrs

# Make the entrypoint executable
RUN ln -s /aegeus-jaxrs/bin/run-aegeus-jaxrs.sh /usr/local/bin/aegeus-jaxrs

CMD ["start"]
ENTRYPOINT ["aegeus-jaxrs"]
EOF

docker rmi -f aegeus/aegeus-jaxrs
docker build -t aegeus/aegeus-jaxrs docker/

docker push aegeus/aegeus-jaxrs

docker tag aegeus/aegeus-jaxrs aegeus/aegeus-jaxrs:$NVERSION
docker push aegeus/aegeus-jaxrs:$NVERSION
```

### Run the AEG JAXRS image

```
export NAME=aeg-jaxrs

docker rm -f $NAME
docker run --detach \
    --link aegd:aeg \
    --link aeg-ipfs:ipfs \
    -p 8081:8081 \
    --memory=200m --memory-swap=2g \
    --name $NAME \
    aegeus/aegeus-jaxrs
    
docker exec -it $NAME tail -f -n 100 debug.log

watch docker logs $NAME

docker exec $NAME aegeus-jaxrs --help
docker exec -it $NAME tail -f -n 100 debug.log
```

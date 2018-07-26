
## Setup the AEG Demo for Marry

The AEG demo requires a working [Docker](https://www.docker.com/community-edition) environment. 

### Run the IPFS image

In the steps below, you would have to replace `167.99.32.85` with the external IP of Marry's host.

```
export NAME=ipfs-02

docker rm -f $NAME
docker run --detach \
    -p 4001:4001 \
    -p $MNEXTIP:8080:8080 \
    --expose 5001 \
    --memory=200m --memory-swap=2g \
    --name $NAME \
    nessusio/ipfs

sleep 6

export EXTERNALIP=167.99.32.85
echo "docker exec ipfs-01 ipfs swarm connect /ip4/$EXTERNALIP/tcp/4001/ipfs/`docker exec $NAME ipfs config Identity.PeerID`"
```

### Run the AEG image

```
export MNNAME=aeg-02
export MNPORT=29328

docker rm -f $MNNAME
docker run --detach \
    -p $MNPORT:$MNPORT \
    --memory=200m --memory-swap=2g \
    --name $MNNAME \
    nessusio/aegeus

watch docker exec $MNNAME aegeus-cli getinfo
```

### Run the AEG JAXRS image

```
export NAME=jaxrs-02
export MNNAME=aeg-02
export IPFSNAME=ipfs-02
export JAXRSPORT=8081

docker rm -f $NAME
docker run --detach \
    -p $JAXRSPORT:$JAXRSPORT \
    --link $MNNAME:aeg \
    --link $IPFSNAME:ipfs \
    --memory=200m --memory-swap=2g \
    --name $NAME \
    nessusio/aegeus-jaxrs
    
watch docker logs jaxrs-02

docker exec $NAME aegeus-jaxrs --help
docker exec -it $NAME tail -f -n 100 debug.log

docker exec $NAME ipfs --api=/ip4/172.17.0.2/tcp/5001 config show
```

### Run the AEG WebUI image

```
export LABEL=Marry
export NAME=webui-02
export MNNAME=aeg-02
export IPFSNAME=ipfs-02
export JAXRSNAME=jaxrs-02
export WEBPORT=5002

docker rm -f $NAME
docker run --detach \
    -p $WEBPORT:$WEBPORT \
    --link $MNNAME:aeg \
    --link $IPFSNAME:ipfs \
    --link $JAXRSNAME:jaxrs \
    --env AEG_WEBUI_LABEL=$LABEL \
    --env AEG_WEBUI_PORT=$WEBPORT \
    --memory=200m --memory-swap=2g \
    --name $NAME \
    nessusio/aegeus-webui
    
watch docker logs $NAME
  
docker exec -it $NAME tail -f -n 100 debug.log
```

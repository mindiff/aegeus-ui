
## Setup Guide

The steps to get the demo running on your box are trivial. It assumes that you already have [Docker](https://www.docker.com/community-edition) running. 
If not, there is guide on how to setup a VPS that has Docker support [here](../setup/Setup-VPS-Docker.md). These steps would however equally work on your Mac or Windows box. 
 
### Run the IPFS image

In the steps below, you would have to replace `167.99.32.83` with the external IP of your host.

```
export NAME=ipfs-01

docker rm -f $NAME
docker run --detach \
    -p 4001:4001 \
    -p 8080:8080 \
    --expose 5001 \
    --memory=200m --memory-swap=2g \
    --name $NAME \
    nessusio/ipfs

sleep 6

export EXTERNALIP=167.99.32.83
echo "docker exec ipfs-02 ipfs swarm connect /ip4/$EXTERNALIP/tcp/4001/ipfs/`docker exec $NAME ipfs config Identity.PeerID`"
```

### Run the AEG image

```
export MNNAME=aeg-01
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
export NAME=jaxrs-01
export MNNAME=aeg-01
export IPFSNAME=ipfs-01
export JAXRSPORT=8081

docker rm -f $NAME
docker run --detach \
    -p $JAXRSPORT:$JAXRSPORT \
    --link $MNNAME:aeg \
    --link $IPFSNAME:ipfs \
    --memory=200m --memory-swap=2g \
    --name $NAME \
    nessusio/aegeus-jaxrs
    
watch docker logs jaxrs-01

docker exec $NAME aegeus-jaxrs --help
docker exec -it $NAME tail -f -n 100 debug.log

docker exec $NAME ipfs --api=/ip4/172.17.0.2/tcp/5001 config show
```

### Run the AEG WebUI image

```
export LABEL=Bob
export NAME=webui-01
export MNNAME=aeg-01
export IPFSNAME=ipfs-01
export JAXRSNAME=jaxrs-01
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
    
watch docker logs webui-01

docker exec -it webui-01 tail -f -n 100 debug.log
```

### Setup for Marry (optional)

For the more advanced __send__ functionality you may want to also follow the [setup for Marry](Setup-Guide-Marry.md).

## Build the AEG IPFS image

### Build the IPFS image

```
export NVERSION=0.4.18

docker pull nessusio/ipfs:$NVERSION
docker tag nessusio/ipfs:$NVERSION aegeus/aegeus-ipfs
docker tag nessusio/ipfs:$NVERSION aegeus/aegeus-ipfs:$NVERSION

docker push aegeus/aegeus-ipfs
docker push aegeus/aegeus-ipfs:$NVERSION
```

### Run the IPFS image 

```
export NAME=ipfs
export GATEWAYIP=185.92.221.103

docker rm -f $NAME
docker run --detach \
    -p 4001:4001 \
    -p 8080:8080 \
    --env GATEWAYIP=$GATEWAYIP \
    --memory=200m --memory-swap=2g \
    --name $NAME \
    aegeus/aegeus-ipfs

docker logs $NAME
```
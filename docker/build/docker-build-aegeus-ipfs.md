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
export CNAME=aeg-ipfs
export GATEWAYIP=192.168.178.20

docker rm -f $CNAME
docker run --detach \
    -p 4001:4001 \
    -p 8080:8080 \
    --env GATEWAYIP=$GATEWAYIP \
    --memory=300m --memory-swap=2g \
    --name $CNAME \
    aegeus/aegeus-ipfs

docker logs aeg-ipfs
```

## Setup the AEG Demo for Marry

The AEG demo requires a working [Docker](https://www.docker.com/community-edition) environment. 

### Quickstart 

In case you know what you're doing already. Here is the quickstart to get the whole system running in no time ... 

    docker run --detach --name aegd -p 29328:29328 --memory=200m --memory-swap=2g aegeus/aegeusd
    docker run --detach --name aeg-ipfs -p 4001:4001 -p 8080:8080 --expose 5001 --memory=200m --memory-swap=2g aegeus/aegeus-ipfs; sleep 20
    docker run --detach --name aeg-jaxrs -p 8081:8081 --link aegd:aeg --link aeg-ipfs:ipfs --memory=200m --memory-swap=2g aegeus/aegeus-jaxrs
    docker run --detach --name aeg-webui -p 8082:8082 --link aegd:aeg --link aeg-ipfs:ipfs --link aeg-jaxrs:jaxrs --memory=200m --memory-swap=2g --env AEG_WEBUI_LABEL=Marry aegeus/aegeus-webui

### Running the AEG daemon image

    docker run --detach \
        -p 29328:29328 \
        --memory=200m --memory-swap=2g \
        --name aegd \
        aegeus/aegeusd

It'll take a little while for the network to sync. You can watch progress like this ...

    watch docker exec aegd aegeus-cli getinfo

### Running the AEG IPFS image

In the steps below, you would have to replace `167.99.32.85` with the external IP of your host.

    docker run --detach \
        -p 4001:4001 \
        -p 8080:8080 \
        --expose 5001 \
        --memory=200m --memory-swap=2g \
        --name aeg-ipfs \
        aegeus/aegeus-ipfs

    sleep 6

    export EXTERNALIP=167.99.32.85
    echo "docker exec aeg-ipfs ipfs swarm connect /ip4/$EXTERNALIP/tcp/4001/ipfs/`docker exec $NAME ipfs config Identity.PeerID`"

### Running the AEG JAXRS image

    docker run --detach \
        -p 8081:8081 \
        --link aegd:aeg \
        --link aeg-ipfs:ipfs \
        --memory=200m --memory-swap=2g \
        --name aeg-jaxrs \
        aegeus/aegeus-jaxrs
    
    watch docker logs aeg-jaxrs

### Running the AEG WebUI image

    docker run --detach \
        -p 8082:8082 \
        --link aegd:aeg \
        --link aeg-ipfs:ipfs \
        --link aeg-jaxrs:jaxrs \
        --env AEG_WEBUI_LABEL=Bob \
        --memory=200m --memory-swap=2g \
        --name aeg-webui \
        aegeus/aegeus-webui
    
    watch docker logs aeg-webui
  
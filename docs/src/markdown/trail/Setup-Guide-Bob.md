
## Setup Guide for Bob

The steps to get the demo running on your box are trivial. It assumes that you already have [Docker](https://www.docker.com/community-edition) running. 
If not, there is guide on how to setup a VPS that has Docker support [here](../setup/Setup-VPS-Docker.md). These steps would however equally work on your Mac or Windows box. 
 
### Quickstart 

In case you know what you're doing already. Here is the quickstart to get the whole system running in no time ... 

    docker run --detach --name aegd -p 29328:29328 --memory=200m --memory-swap=2g aegeus/aegeusd
    docker run --detach --name ipfs -p 4001:4001 -p 8080:8080 --memory=200m --memory-swap=2g aegeus/aegeus-ipfs; sleep 20
    docker run --detach --name jaxrs -p 8081:8081 --link aegd:aeg --link ipfs:ipfs --memory=200m --memory-swap=2g aegeus/aegeus-jaxrs
    docker run --detach --name webui -p 8082:8082 --link aegd:aeg --link ipfs:ipfs --link jaxrs:jaxrs --memory=200m --memory-swap=2g --env AEG_WEBUI_LABEL=Bob aegeus/aegeus-webui

### Running the AEG daemon image

    docker run --detach \
        -p 29328:29328 \
        --memory=200m --memory-swap=2g \
        --name aegd \
        aegeus/aegeusd

It'll take a little while for the network to sync. You can watch progress like this ...

    watch docker exec aegd aegeus-cli getinfo

### Running the AEG IPFS image

In the steps below, you would have to replace `185.92.221.103` with the external IP of your host.

    docker run --detach \
        -p 4001:4001 \
        -p 8080:8080 \
        --memory=200m --memory-swap=2g \
        --name ipfs \
        aegeus/aegeus-ipfs

    sleep 6

    export GATEWAYIP=185.92.221.103
    echo "docker exec ipfs ipfs swarm connect /ip4/$GATEWAYIP/tcp/4001/ipfs/`docker exec $NAME ipfs config Identity.PeerID`"

### Running the AEG JAXRS image

    docker run --detach \
        -p 8081:8081 \
        --link aegd:aeg \
        --link ipfs:ipfs \
        --memory=200m --memory-swap=2g \
        --name jaxrs \
        aegeus/aegeus-jaxrs
    
    watch docker logs jaxrs

### Running the AEG WebUI image

    docker run --detach \
        -p 8082:8082 \
        --link aegd:aeg \
        --link ipfs:ipfs \
        --link jaxrs:jaxrs \
        --env AEG_WEBUI_LABEL=Bob \
        --memory=200m --memory-swap=2g \
        --name webui \
        aegeus/aegeus-webui
    
    watch docker logs webui

### Setup for Mary (optional)

For the more advanced __send__ functionality you may want to also follow the [setup for Mary](Setup-Guide-Mary.md).

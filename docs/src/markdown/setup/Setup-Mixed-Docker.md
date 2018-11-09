## Local setup with mixed services 

This type of installation requires a working [Docker](https://www.docker.com/community-edition#/download) environment.

To verify that your Docker env is setup properly, you can list the running container like this ...

    $ docker ps
    CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS               NAMES

In total there are four Docker images to make up the complete system.

1. [aegeus/aegeusd](https://hub.docker.com/r/aegeus/aegeusd)
2. [aegeus/aegeus-ipfs](https://hub.docker.com/r/aegeus/aegeus-ipfs)
3. [aegeus/aegeus-jaxrs](https://hub.docker.com/r/aegeus/aegeus-jaxrs)
4. [aegeus/aegeus-webui](https://hub.docker.com/r/aegeus/aegeus-webui)

What follows is an installation guide for the last three containers. 
It is assumed that you already have a local AEG wallet running. 

### Running the IPFS image

To start the Aegeus IPFS daemon in Docker, you can run ...

```
export NAME=ipfs
export GATEWAYIP=192.168.178.20

docker rm -f $NAME
docker run --detach \
    -p 4001:4001 \
    -p 8080:8080 \
    --env GATEWAYIP=$GATEWAYIP \
    --memory=200m --memory-swap=2g \
    --name $NAME \
    aegeus/aegeus-ipfs
```

### Running the AEG JAXRS image

This is the JSON-RPC bridge, which contains the Aegeus application logic that connects the Aegeus network with IPFS network. 

#### Bind the Aegeus wallet to an external IP

For this to work, your Aegeus wallet needs to bind to an external IP

    server=1
    txindex=1
    rpcuser=aeg
    rpcpassword=aegpass
    rpcbind=192.168.178.20
    rpcallowip=192.168.178.20
    rpcconnect=192.168.178.20
    rpcport=51473
    wallet=test-wallet.dat                                                                                                                                                                                                  
 
Verify that this works

    export LOCALIP=192.168.178.20
    curl --data-binary '{"method": "getinfo"}' http://aeg:aegpass@$LOCALIP:51473
    
Then, verify that this also works from within docker

    docker run -it --rm --entrypoint=bash aegeus/aegeus-jaxrs
    
    export LOCALIP=192.168.178.20
    curl --data-binary '{"method": "getinfo"}' http://aeg:aegpass@192.168.178.20:51473
    
#### Run the AEG JAXRS image

To start the Aegeus bridge in Docker, you can run ...
    
    export LOCALIP=192.168.178.20
    
    docker run --detach \
        -p 8081:8081 \
        --link ipfs:ipfs \
        --env AEG_PORT_51473_TCP_ADDR=$LOCALIP \
        --env AEG_PORT_51473_TCP_PORT=51473 \
        --env AEG_ENV_RPCUSER=aeg \
        --env AEG_ENV_RPCPASS=aegpass \
        --memory=200m --memory-swap=2g \
        --name jaxrs \
        aegeus/aegeus-jaxrs

On bootstrap the bridge reports some connection properties.

    docker logs jaxrs
    
    AegeusBlockchain: http://aeg:*******@192.168.178.20:51473
    AegeusNetwork Version: 2000000
    IPFS Version: 0.4.16
    Aegeus JAXRS: http://0.0.0.0:8081/aegeus

### Running the AEG WebUI image

In this setup the Aegeus UI is optional as well. Still, lets try to connect it to the JSON-RPC bridge and the Aegeus wallet  ...

    export LABEL=Mary
    
    docker run --detach \
        -p 8082:8082 \
        --link ipfs:ipfs \
        --link jaxrs:jaxrs \
        --env AEG_PORT_51473_TCP_ADDR=$LOCALIP \
        --env AEG_PORT_51473_TCP_PORT=51473 \
        --env AEG_ENV_RPCUSER=aeg \
        --env AEG_ENV_RPCPASS=aegpass \
        --env AEG_WEBUI_LABEL=$LABEL \
        --memory=200m --memory-swap=2g \
        --name webui \
        aegeus/aegeus-webui

Now that everything is running, it should look like this

    docker ps
    
    CONTAINER ID        IMAGE                 COMMAND                CREATED             STATUS              PORTS                                                      NAMES
    508d3074fb89        aegeus/aegeus-webui   "aegeus-webui"         4 seconds ago       Up 3 seconds        0.0.0.0:8082->8082/tcp                                     webui
    07cf9212507c        aegeus/aegeus-jaxrs   "aegeus-jaxrs start"   3 minutes ago       Up 3 minutes        0.0.0.0:8081->8081/tcp                                     jaxrs
    fded955826a8        nessusio/ipfs         "nessusio-ipfs"        13 minutes ago      Up 13 minutes       0.0.0.0:4001->4001/tcp, 5001/tcp, 0.0.0.0:8080->8080/tcp   ipfs

The WebUI also reports some connection properties.

    docker logs webui
    
    AEG JAXRS: http://172.17.0.3:8081/aegeus
    IPFS Gateway: http://192.168.178.20:8080/ipfs
    AEG WebUI: http://0.0.0.0:8082/portal
    AegeusBlockchain: http://aeg:*******@192.168.178.20:51473
    AegeusNetwork Version: 3000000

You should now be able to access the WebUI at: [http://127.0.0.1:8082/portal](http://127.0.0.1:8082/portal)

Everything else should work as described [here](Setup-All-Docker.md).
## Local setup with mixed services 

This type of installation requires a working [Docker](https://www.docker.com/community-edition#/download) environment.

To verify that your Docker env is setup properly, you can list the running container like this ...

    docker ps

    CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS               NAMES

In total there are four Docker images to make up the complete system.

1. [nessusio/ipfs](https://hub.docker.com/r/nessusio/ipfs)
2. [nessusio/aegeus](https://hub.docker.com/r/nessusio/aegeus)
3. [nessusio/aegeus-jaxrs](https://hub.docker.com/r/nessusio/aegeus-jaxrs)
4. [nessusio/aegeus-webui](https://hub.docker.com/r/nessusio/aegeus-webui)

What follows is an installation guide for the last two containers. 
It is assumed that you already have a local IPFS and AEG wallet running. 

#### nessusio/aegeus-jaxrs

This is the JSON-RPC bridge, which contains the Aegeus application logic that connects the Aegeus network with IPFS network. 

For this to work, your Aegeus client needs to bind to an external IP

    server=1
    txindex=1
    rpcuser=aeg
    rpcpassword=aegpass
    rpcbind=192.168.178.20
    rpcallowip=192.168.178.20
    rpcport=51473
    wallet=test-wallet.dat                                                                                                                                                                                                  
 
Verify that this works

    export LOCALIP=192.168.178.20
    curl --data-binary '{"method": "getinfo"}' http://aeg:aegpass@$LOCALIP:51473
    
Then, verify that this also works from within docker

    docker run -it --entrypoint=bash fedora:28
    curl --data-binary '{"method": "getinfo"}' http://aeg:aegpass@192.168.178.20:51473
    
To start the Aegeus bridge in Docker, you can run ...
    
    docker run --detach \
        -p 8081:8081 \
        --env IPFS_PORT_5001_TCP_ADDR=$LOCALIP \
        --env IPFS_PORT_5001_TCP_PORT=5001 \
        --env IPFS_PORT_8080_TCP_ADDR=$LOCALIP \
        --env IPFS_PORT_8080_TCP_PORT=8080 \
        --env AEG_PORT_51473_TCP_ADDR=$LOCALIP \
        --env AEG_PORT_51473_TCP_PORT=51473 \
        --env AEG_ENV_RPCUSER=aeg \
        --env AEG_ENV_RPCPASS=aegpass \
        --name jaxrs \
        nessusio/aegeus-jaxrs

On bootstrap the bridge reports some connection properties.

    docker logs jaxrs
    
    AegeusBlockchain: http://aeg:*******@192.168.178.20:51473
    AegeusNetwork Version: 2000000
    IPFS Version: 0.4.16
    Aegeus JAXRS: http://0.0.0.0:8081/aegeus

#### nessusio/aegeus-webui

In this setup the Aegeus UI is optional as well. Still, lets try to connect it to the JSON-RPC bridge and the Aegeus wallet  ...

    docker run --detach \
        -p 8082:8082 \
        --link jaxrs:jaxrs \
        --env IPFS_PORT_8080_TCP_ADDR=$LOCALIP \
        --env IPFS_PORT_8080_TCP_PORT=8080 \
        --env AEG_PORT_51473_TCP_ADDR=$LOCALIP \
        --env AEG_PORT_51473_TCP_PORT=51473 \
        --env AEG_ENV_RPCUSER=aeg \
        --env AEG_ENV_RPCPASS=aegpass \
        --env AEG_WEBUI_LABEL=Bob \
        --name webui \
        nessusio/aegeus-webui

Now that everything is running, it should look like this

    docker ps
    
    CONTAINER ID        IMAGE                   COMMAND                  CREATED             STATUS              PORTS                    NAMES
    417b2518b499        nessusio/aegeus-webui   "aegeus-webui/bin/ru…"   9 seconds ago       Up 8 seconds        0.0.0.0:8082->8082/tcp   webui
    e1a0bbf693fa        nessusio/aegeus-jaxrs   "aegeus-jaxrs start"     3 minutes ago       Up 3 minutes        0.0.0.0:8081->8081/tcp   jaxrs

The WebUI also reports some connection properties.

    docker logs webui
    
    AEG JAXRS: http://172.17.0.2:8081/aegeus
    IPFS Gateway: http://192.168.178.20:8080/ipfs
    AEG WebUI: http://0.0.0.0:8082/portal
    AegeusBlockchain: http://aeg:*******@192.168.178.20:51473
    AegeusNetwork Version: 2000000

You should now be able to access the WebUI at: [http://127.0.0.1:8082/portal](http://127.0.0.1:8082/portal)

Everything else should work as described [here](Setup-Local-All-Docker.md).
## Welcome to the Aegeus IPFS system 

Here we proudly bring you a preview of the Aegeus user interface.

![preview](docs/src/markdown/trail/img/bob-list-03-small.png)

A full walk through of the demo is here: [QmU4F4omxJhsQKktKVDjcS8Edpj7m96w1Y9zCcoYr8FsVJ](https://ipfs.io/ipfs/QmU4F4omxJhsQKktKVDjcS8Edpj7m96w1Y9zCcoYr8FsVJ/trail)

### Installing Docker 

Currently, this installation requires a working [Docker](https://www.docker.com/community-edition#/download) environment.

To verify that your Docker env is setup properly, you can list the running containers like this ...

    $ docker ps
    CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS               NAMES

### The Aegeus images
 
In total there are four Docker images to make up the complete system.

1. [aegeus/aegeusd](https://hub.docker.com/r/aegeus/aegeusd)
2. [aegeus/aegeus-ipfs](https://hub.docker.com/r/aegeus/aegeus-ipfs)
3. [aegeus/aegeus-jaxrs](https://hub.docker.com/r/aegeus/aegeus-jaxrs)
4. [aegeus/aegeus-webui](https://hub.docker.com/r/aegeus/aegeus-webui)

What follows is an installation guide for all four containers. However, if you already have IPFS and AEG running locally, you will not need to run these in Docker again.
For a mixed setup with already running IPFS & AEG service and newly hosted Docker services go [here](docker/setup/Setup-Mixed-Docker.md). 

### Quickstart 

In case you know what you're doing already. Here is the quickstart to get the whole system running in no time ... 

    export GATEWAYIP=185.92.221.103
    docker run --detach --name aegd -p 29328:29328 --memory=200m --memory-swap=2g aegeus/aegeusd
    docker run --detach --name ipfs -p 4001:4001 -p 8080:8080 -e GATEWAYIP=$GATEWAYIP --memory=200m --memory-swap=2g aegeus/aegeus-ipfs; sleep 20
    docker run --detach --name jaxrs -p 8081:8081 --link aegd:aeg --link ipfs:ipfs --memory=200m --memory-swap=2g aegeus/aegeus-jaxrs
    docker run --detach --name webui -p 8082:8082 --link aegd:aeg --link ipfs:ipfs --link jaxrs:jaxrs --memory=200m --memory-swap=2g --env AEG_WEBUI_LABEL=Bob aegeus/aegeus-webui

You should now be able to access the WebUI at: [http://127.0.0.1:8082/portal](http://127.0.0.1:8082/portal)

### Running the Aegeus daemon

To start the Aegeus daemon in Docker, you can run ...

    docker run --detach \
        -p 29328:29328 \
        --memory=200m --memory-swap=2g \
        --name aegd \
        aegeus/aegeusd

It'll take a little while for the network to sync. You can watch progress like this ...

    watch docker exec aegd aegeus-cli getinfo

### Running the Aegeus IPFS daemon

To start the Aegeus IPFS daemon in Docker, you can run ...

    export GATEWAYIP=185.92.221.103
    
    docker run --detach \
        -p 4001:4001 \
        -p 8080:8080 \
        --env GATEWAYIP=$GATEWAYIP \
        --memory=200m --memory-swap=2g \
        --name ipfs \
        aegeus/aegeus-ipfs

In case you need to connect the IPFS swarm to this instance, you can get the network ID like this ...

    echo "ipfs swarm connect /ip4/$GATEWAYIP/tcp/4001/ipfs/`docker exec ipfs ipfs config Identity.PeerID`"
    
and then on some other IPFS instance connect to the Aegeus IPFS daemon like this ...

    ipfs swarm connect /ip4/185.92.221.103/tcp/4001/ipfs/QmabAtE8qXJKDJ3SnxX18ZfEg9xMKdqoiA3KhW58hi4pmL

You can always get the system out for a running service like this ...

    docker logs ipfs
    
    initializing IPFS node at /root/.ipfs
    generating 2048-bit RSA keypair...done
    peer identity: QmabAtE8qXJKDJ3SnxX18ZfEg9xMKdqoiA3KhW58hi4pmL
    to get started, enter:
    
        ipfs cat /ipfs/QmS4ustL54uo8FzR9455qaxZwuMiUhyvMcX9Ba8nUH4uVv/readme
    
    Initializing daemon...
    Swarm listening on /ip4/127.0.0.1/tcp/4001
    Swarm listening on /ip4/172.17.0.2/tcp/4001
    Swarm listening on /p2p-circuit/ipfs/QmabAtE8qXJKDJ3SnxX18ZfEg9xMKdqoiA3KhW58hi4pmL
    Swarm announcing /ip4/127.0.0.1/tcp/4001
    Swarm announcing /ip4/172.17.0.2/tcp/4001
    API server listening on /ip4/0.0.0.0/tcp/5001
    Gateway (readonly) server listening on /ip4/0.0.0.0/tcp/8080
    Daemon is ready

### Running the AEG JAXRS image

This is the Aegeus JSON-RPC bridge, which contains the Aegeus application logic that connects the Aegeus network with IPFS network. 

To start the Aegeus bridge in Docker, you can run ...

    docker run --detach \
        -p 8081:8081 \
        --link aegd:aeg \
        --link ipfs:ipfs \
        --memory=200m --memory-swap=2g \
        --name jaxrs \
        aegeus/aegeus-jaxrs

On bootstrap the bridge reports some connection properties.

    docker logs jaxrs
    
    AegeusBlockchain: http://aeg:*******@172.17.0.3:51473
    AegeusNetwork Version: 2000000
    IPFS Version: 0.4.16
    Aegeus JAXRS: http://0.0.0.0:8081/aegeus

### Running the AEG WebUI image

This is a prototype of the Aegeus UI. 

To start up the Aegeus UI in Docker, you can run ...

    docker run --detach \
        -p 8082:8082 \
        --link aegd:aeg \
        --link ipfs:ipfs \
        --link jaxrs:jaxrs \
        --memory=200m --memory-swap=2g \
        --env AEG_WEBUI_LABEL=Bob \
        --name webui \
        aegeus/aegeus-webui

Now that everything is running, it should look like this

    docker ps
    
    CONTAINER ID        IMAGE                 COMMAND                  CREATED             STATUS              PORTS                                                      NAMES
    4c11bf22f1bf        aegeus/aegeus-webui   "aegeus-webui"           5 seconds ago       Up 4 seconds        0.0.0.0:8082->8082/tcp                                     webui
    674f14f7f56e        aegeus/aegeus-jaxrs   "aegeus-jaxrs start"     8 minutes ago       Up 8 minutes        0.0.0.0:8081->8081/tcp                                     jaxrs
    980564af7cc4        aegeus/aegeus-ipfs    "aegeus-ipfs"            46 minutes ago      Up 46 minutes       0.0.0.0:4001->4001/tcp, 0.0.0.0:8080->8080/tcp, 5001/tcp   ipfs
    cbde5c778206        aegeus/aegeusd        "aegeusd -datadir=..."   About an hour ago   Up About an hour    0.0.0.0:29328->29328/tcp, 51473/tcp                        aegd

The WebUI also reports some connection properties.

    docker logs webui
    
    AEG JAXRS: http://172.17.0.4:8081/aegeus
    IPFS Gateway: http://185.92.221.103:8080/ipfs
    AEG WebUI: http://0.0.0.0:8082/portal
    AegeusBlockchain: http://aeg:*******@172.17.0.3:51473
    AegeusNetwork Version: 2000000

You should now be able to access the WebUI at: [http://127.0.0.1:8082/portal](http://127.0.0.1:8082/portal)

### Updating the installation

Remove all running containers

    docker rm -f `docker ps -aq`

Pull the latest image versions

    docker pull aegeus/aegeusd
    docker pull aegeus/aegeus-ipfs
    docker pull aegeus/aegeus-jaxrs
    docker pull aegeus/aegeus-webui

Then, start again by running these containers.



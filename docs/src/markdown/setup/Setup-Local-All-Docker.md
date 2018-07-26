## Local setup with Docker services 

This type of installation requires a working [Docker](https://www.docker.com/community-edition#/download) environment.

To verify that your Docker env is setup properly, you can list the running container like this ...

    docker ps

    CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS               NAMES

In total there are four Docker images to make up the complete system.

1. [nessusio/ipfs](https://hub.docker.com/r/nessusio/ipfs)
2. [nessusio/aegeus](https://hub.docker.com/r/nessusio/aegeus)
3. [nessusio/aegeus-jaxrs](https://hub.docker.com/r/nessusio/aegeus-jaxrs)
4. [nessusio/aegeus-webui](https://hub.docker.com/r/nessusio/aegeus-webui)

What follows is an installation guide for all four containers. However, if you already have IPFS and AEG running locally, you will not need to run these in Docker again.
The WebUI is also optional. The only container that is required is the JSON-RPC bridge i.e. `nessusio/aegeus-jaxrs`. Having said that, it is of course also possible to
run the brige as a standalone java application, in which case you won't need Docker at all.

For convenience however, lets do the whole setup in Docker first.

#### nessusio/ipfs

This is the IPFS system. To start up IPFS in Docker, you can run ...

    docker run --detach \
        -p 4001:4001 \
        -p 8080:8080 \
        --expose 5001 \
        --name ipfs \
        nessusio/ipfs

When this is done, you should see

    docker ps
    
    CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS                                                      NAMES
    b47c30986ca7        nessusio/ipfs       "/root/run.sh"      4 seconds ago       Up 4 seconds        0.0.0.0:4001->4001/tcp, 0.0.0.0:8080->8080/tcp, 5001/tcp   ipfs

In case you need to connect the IPFS swarm to this instance, you can get the network ID like this ...

    export EXTERNALIP=192.168.178.20
    echo "ipfs swarm connect /ip4/$EXTERNALIP/tcp/4001/ipfs/`docker exec ipfs ipfs config Identity.PeerID`"
    
    ipfs swarm connect /ip4/192.168.178.20/tcp/4001/ipfs/QmabAtE8qXJKDJ3SnxX18ZfEg9xMKdqoiA3KhW58hi4pmL

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

#### nessusio/aegeus

This is the Aegeus network daemon with RPC access to the wallet. To start up Aegeus in Docker, you can run ...

    docker run --detach \
        -p 29328:29328 \
        --name aeg \
        nessusio/aegeus

It'll take a little while for the network to sync. You can watch progress like this

    watch docker exec aeg aegeus-cli getinfo

#### nessusio/aegeus-jaxrs

This is the JSON-RPC bridge, which contains the Aegeus application logic that connects the Aegeus network with IPFS network. To start the Aegeus bridge in Docker, you can run ...

    docker run --detach \
        -p 8081:8081 \
        --link aeg:aeg \
        --link ipfs:ipfs \
        --name jaxrs \
        nessusio/aegeus-jaxrs

On bootstrap the bridge reports some connection properties.

    docker logs jaxrs
    
    AegeusBlockchain: http://aeg:*******@172.17.0.3:51473
    AegeusNetwork Version: 2000000
    IPFS Version: 0.4.16
    Aegeus JAXRS: http://0.0.0.0:8081/aegeus

Now, lets have a look at the available JSON-RPC methods

    docker exec jaxrs aegeus-jaxrs --help
    
    Aegeus JAXRS
    ============
     
    Commandline Options
    -------------------
     
    * aegeus-jaxrpc start - Starts the JAXRS server
    * aegeus-jaxrpc stop  - Stops the JAXRS server
     
    REST API
    --------
     
        @GET
        @Path("/register")
        @Produces(MediaType.TEXT_PLAIN)
        String register(@QueryParam("addr") String rawAddr)
     
        @POST
        @Path("/add")
        @Produces(MediaType.APPLICATION_JSON)
        SFHandle add(@QueryParam("addr") String rawAddr, @QueryParam("path") String path, InputStream input)
         
        @GET
        @Path("/get")
        @Produces(MediaType.APPLICATION_JSON)
        SFHandle get(@QueryParam("addr") String rawAddr, @QueryParam("cid") String cid, @QueryParam("path") String path, @QueryParam("timeout") Long timeout)
     
        @GET
        @Path("/send")
        @Produces(MediaType.APPLICATION_JSON)
        SFHandle send(@QueryParam("addr") String rawAddr, @QueryParam("cid") String cid, @QueryParam("target") String rawTarget, @QueryParam("timeout") Long timeout)
     
        @GET
        @Path("/findkey")
        @Produces(MediaType.APPLICATION_JSON)
        String findRegistation(@QueryParam("addr") String rawAddr)
     
        @GET
        @Path("/findipfs")
        @Produces(MediaType.APPLICATION_JSON)
        List<SFHandle> findIPFSContent(@QueryParam("addr") String rawAddr, @QueryParam("timeout") Long timeout)
     
        @GET
        @Path("/findlocal")
        @Produces(MediaType.APPLICATION_JSON)
        List<SFHandle> findLocalContent(@QueryParam("addr") String rawAddr)
         
        @GET
        @Path("/getlocal")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        InputStream getLocalContent(@QueryParam("addr") String rawAddr, @QueryParam("path") String path)
         
        @GET
        @Path("/dellocal")
        @Produces(MediaType.TEXT_PLAIN)
        boolean deleteLocalContent(@QueryParam("addr") String rawAddr, @QueryParam("path") String path)

Before we connect to the bridge directly, lets first take look at the WebUI and do some inital setup. 

#### nessusio/aegeus-webui

This is a prototype of the Aegeus UI. To start up the Aegeus UI in Docker, you can run ...

    docker run --detach \
        -p 8082:8082 \
        --link aeg:aeg \
        --link ipfs:ipfs \
        --link jaxrs:jaxrs \
        --env AEG_WEBUI_LABEL=Bob \
        --name webui \
        nessusio/aegeus-webui

Now that everything is running, it should look like this

    docker ps
    
    CONTAINER ID        IMAGE                   COMMAND                  CREATED             STATUS              PORTS                                                      NAMES
    d3af7e528589        nessusio/aegeus-webui   "aegeus-webui/bin/ru…"   5 seconds ago       Up 4 seconds        0.0.0.0:8082->8082/tcp                                     webui
    b7e0ca5e3e47        nessusio/aegeus-jaxrs   "aegeus-jaxrs start"     52 seconds ago      Up 50 seconds       0.0.0.0:8081->8081/tcp                                     jaxrs
    41c234037fd8        nessusio/aegeus         "aegeusd -datadir=/v…"   5 minutes ago       Up 5 minutes        0.0.0.0:29328->29328/tcp, 51473/tcp                        aeg
    758b96812f88        nessusio/ipfs           "/root/run.sh"           14 minutes ago      Up 14 minutes       0.0.0.0:4001->4001/tcp, 0.0.0.0:8080->8080/tcp, 5001/tcp   ipfs

The WebUI also reports some connection properties.

    docker logs webui
    
    AEG JAXRS: http://172.17.0.4:8081/aegeus
    IPFS Gateway: http://172.17.0.2:8080/ipfs
    AEG WebUI: http://0.0.0.0:8082/portal
    AegeusBlockchain: http://aeg:*******@172.17.0.3:51473
    AegeusNetwork Version: 2000000

You should now be able to access the WebUI at: [http://127.0.0.1:8082/portal](http://127.0.0.1:8082/portal)

### Working with JSON-RPC

Lets assume we don't want to use the WebUI, but instead use JSON-RPC calls only. Lets try ...

First, lets import a known AEG private key, so that Bob get some funds. This will take a while because the wallet needs to reindex the transactions.

    docker exec aeg aegeus-cli importprivkey "PRPS5pH***********************" Bob

On success, there is no feedback from this command. 
When you again look at the WebUI, you should see that Bob's address is now assigned to an account and that he has some funds.

Next, lets register Bob's address with the system by making a JSON-RPC call to the bridge.

    curl http://127.0.0.1:8081/aegeus/register?addr=AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX
    
    MDYwEAYHKoZIzj0CAQYFK4EEABwDIgAEP2ru5CB0CMyGX4sp8hz5qVn4eKgSo8NpToww2bAksZg=

This is Bob's public key used for IPFS file encryption. 

Lets see, if we can also retrieve it from the blockchain.

    curl http://127.0.0.1:8081/aegeus/findkey?addr=AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX
    
    MDYwEAYHKoZIzj0CAQYFK4EEABwDIgAEP2ru5CB0CMyGX4sp8hz5qVn4eKgSo8NpToww2bAksZg=

Now, lets add this document to the system.

    echo "Hello World" > test.txt
    curl --request POST --data @test.txt http://127.0.0.1:8081/aegeus/add?addr=AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX\&path=test.txt
    
    {
        "owner":"AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX",
        "path":"test.txt",
        "cid":"QmWjYT1TunvTPTRrAE3nVTLExV4e72EVpwS9JaqbH6ba59",
        "encrypted":true
    }

Again, this should be reflected in the WebUI.

Connecting to the IPFS gateway direcly, we should be able to see the file content.

    curl http://127.0.0.1:8080/ipfs/QmWjYT1TunvTPTRrAE3nVTLExV4e72EVpwS9JaqbH6ba59
    
    AEG-Version: 1.0
    Path: test.txt
    Owner: AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX
    Token: BFv3LZ/mgq3VB0M8Y80GtF+t6TrRq4/FaG1dX3uqwghE1lEEnQTo3HkLTe907MRPP37eIgGxgJuIdb7OQxgI2G0fzxCy
    AEG_HEADER_END
    AAAADHGC7poPuTyDGLiytI9vHzrH2jgSWMZNGgzq8caW71n+OCognQBMKg==

Like above with the public encryption key, we should be able to find this IPFS content id on the blockhain.

    curl http://127.0.0.1:8081/aegeus/findipfs?addr=AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX
    
    {
        "owner":"AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX",
        "path":"test.txt",
        "cid":"QmWjYT1TunvTPTRrAE3nVTLExV4e72EVpwS9JaqbH6ba59",
        "encrypted":true
    }

The local unencrypted content is also available after the IPFS add.

    curl http://127.0.0.1:8081/aegeus/findlocal?addr=AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX
    
    {
        "owner":"AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX",
        "path":"test.txt",
        "cid":null,
        "encrypted":false}
    }

Lets delete that local file.

    curl http://127.0.0.1:8081/aegeus/dellocal?addr=AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX\&path=test.txt
    curl http://127.0.0.1:8081/aegeus/findlocal?addr=AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX
    
    []

Lets assume at a later time, we would like to get that file from IPFS
 
    curl http://127.0.0.1:8081/aegeus/get?addr=AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX\&path=other.txt\&cid=QmWjYT1TunvTPTRrAE3nVTLExV4e72EVpwS9JaqbH6ba59
    
    {
        "owner":"AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX",
        "path":"other.txt",
        "cid":null,
        "encrypted":false}
    }

Finally, lets get the unencrypted content back

    curl http://127.0.0.1:8081/aegeus/getlocal?addr=AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX\&path=other.txt
    
    Hello World

That's it - Enjoy!

-- thomas

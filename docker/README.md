# Building docker images

You probably don't need to build this - we build the image on every merge to master, and have tagged docker images for each release! This documentation is useful for people who need to build the docker container locally - maintainers, not users. If you are working on this code and want to build a docker image, or you're just curious, read on! 

### Building and running locally
For now, we only produce one docker image, which runs the RemoteActorSystem to listen for islands being sent by an IslandManager. To build this docker image, you have to build the jar, then run `docker build` using the top-level directory as the build context. Run this from the top level directory of evvo:
```bash 
mvn clean package
docker build --build-arg JAR_FILE=evvo-0.0.0-allinone.jar -t evvo:latest -f docker/Dockerfile .
```

To run it:
```bash
docker run -p 3449:3449 \
    --env CLUSTER_PORT=3449 \
    --env CLUSTER_IP=<your local ip> \
    --name RemoteActorSystem evvo:latest
```

To get the value of `CLUSTER_IP`, you can run `ifconfig en0 | grep 'inet '`, or go to [https://www.whatismyip.com/](https://www.whatismyip.com/) and read "Your Local IP is:".


### Deploying to GCP
First, configure your [`gcloud` CLI](https://cloud.google.com/container-registry/docs/advanced-authentication) by running:
```bash
gcloud auth configure-docker
gcloud config set project totemic-cursor-241919
```
You will only have to configure `gcloud` to work with docker once.

#### Pushing to Google Container Registry
For now, this documentation is specific to the Evvo team. To deploy to our GCR:
```bash
docker tag evvo:latest gcr.io/totemic-cursor-241919/ras
docker push gcr.io/totemic-cursor-241919/ras
```

#### Running on a Compute Engine instance
Akka handles networking pretty well, so this runs fine without Kubernetes. It's something we're considering for the future, though, and let us know if it's a feature you want. To create a Compute Engine instance running the image, run:

```bash
INSTANCE_NAME=evvo-instance-1
PROJECT_NAME=totemic-cursor-241919

gcloud beta compute \
    --project=$PROJECT_NAME instances create-with-container $INSTANCE_NAME \
    --zone=us-east4-c \
    --machine-type=n1-standard-1 \
    --subnet=default \
    --network-tier=PREMIUM \
    --metadata=google-logging-enabled=true \
    --maintenance-policy=MIGRATE \
    --service-account=364216542414-compute@developer.gserviceaccount.com \
    --scopes=https://www.googleapis.com/auth/devstorage.read_only,https://www.googleapis.com/auth/logging.write,https://www.googleapis.com/auth/monitoring.write,https://www.googleapis.com/auth/servicecontrol,https://www.googleapis.com/auth/service.management.readonly,https://www.googleapis.com/auth/trace.append \
    --tags=http-server,https-server \
    --image=cos-stable-74-11895-125-0 \
    --image-project=cos-cloud \
    --boot-disk-size=10GB \
    --boot-disk-type=pd-standard \
    --boot-disk-device-name=$INSTANCE_NAME \
    --container-image=gcr.io/$PROJECT_NAME/ras:latest \
    --container-restart-policy=always \
    --labels=container-vm=cos-stable-74-11895-125-0
```

Make sure that ingress and egress traffic is allowed on port 3449.


#### Running with `Remoting`
To ensure that the nodes are set up correctly, run an IslandManager that connects to them. Make sure to change `IslandManager.nodes.locations` to point at your nodes.

# POC on spinnaker and eks with a spring boot project.
Based on https://aws.amazon.com/blogs/opensource/continuous-delivery-spinnaker-amazon-eks/

## Install kubectl and aws-iam-authenticator
```curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl

chmod +x ./kubectl

sudo mv ./kubectl /usr/local/bin/kubectl

curl -o aws-iam-authenticator https://amazon-eks.s3-us-west-2.amazonaws.com/1.13.7/2019-06-11/bin/linux/amd64/aws-iam-authenticator

chmod +x ./aws-iam-authenticator

mkdir -p $HOME/bin && cp ./aws-iam-authenticator $HOME/bin/aws-iam-authenticator && export PATH=$HOME/bin:$PATH

echo 'export PATH=$HOME/bin:$PATH' >> ~/.bashrc

aws-iam-authenticator  help```

## install awscli

```
sudo apt update
sudo apt install python3-pip
pip3 install awscli --upgrade --user
```
If the above one does not work, and it did not, install it from here. the above does not seem to work
https://docs.aws.amazon.com/cli/latest/userguide/install-linux.html
Before that run ```sudo apt install python-pip``` Python3-pip does not work with the last command in this

## Install eksctl
```
curl --silent --location "https://github.com/weaveworks/eksctl/releases/download/latest_release/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin
```

## Install terraform
```
sudo apt install unzip	
wget https://releases.hashicorp.com/terraform/0.12.4/terraform_0.12.4_linux_amd64.zip
unzip terraform_0.12.4_linux_amd64.zip
sudo mv terraform /usr/local/bin/
export PATH=$PATH:/usr/local/bin/terraform
```

## Install halyard
```
curl -O https://raw.githubusercontent.com/spinnaker/halyard/master/install/debian/InstallHalyard.sh
sudo bash InstallHalyard.sh
sudo update-halyard
hal -v
```

## install docker
https://docs.docker.com/engine/install/ubuntu/

## install minikube
```sudo apt-get install conntrack```
https://kubernetes.io/docs/tasks/tools/install-minikube/
To start minikube use this command to see extra logs and to start it without HV
```sudo minikube start --driver=none --alsologtostderr -v=2```

## Create EKS clusters - prod, uat, and one for spinnaker (dont know why that is required)
aws configure is where the first install of awscli did not work.
```
aws configure
eksctl create cluster --name=goda-eks-prod --nodes=3 --region=ap-southeast-2 --write-kubeconfig=false

eksctl create cluster --name=goda-eks-spinnaker-2952020 --nodes=2 --region=ap-southeast-2 --write-kubeconfig=false
```

## Retrieve Amazon EKS cluster kubectl contexts
```
sudo aws eks update-kubeconfig --name goda-eks-spinnaker-2952020 --region ap-southeast-2 --alias goda-eks-spinnaker-2952020
sudo aws eks update-kubeconfig --name goda-eks-prod --region ap-southeast-2 --alias goda-eks-prod
```

## Create and configure a Docker registry
```
sudo hal config provider docker-registry enable 
sudo hal config provider docker-registry account add godaprojects-docker-account --address index.docker.io --username godaprojects --password
```

## Add and configure Kubernetes accounts
```
hal config provider kubernetes enable
sudo kubectl config use-context goda-eks-prod

CONTEXT=$(sudo kubectl config current-context)
kubectl apply --context $CONTEXT \
    -f https://spinnaker.io/downloads/kubernetes/service-account.yml
	
TOKEN=$(sudo kubectl get secret --context $CONTEXT \
   $(sudo kubectl get serviceaccount spinnaker-service-account \
       --context $CONTEXT \
       -n spinnaker \
       -o jsonpath='{.secrets[0].name}') \
   -n spinnaker \
   -o jsonpath='{.data.token}' | base64 --decode)
   
sudo kubectl config set-credentials ${CONTEXT}-token-user --token $TOKEN
sudo kubectl config set-context $CONTEXT --user ${CONTEXT}-token-user
hal config provider kubernetes account add goda-eks-prod --provider-version v2 \
 --docker-registries godaprojects-docker-account --context $CONTEXT
hal config provider kubernetes account add goda-eks-spinnaker-2952020 --provider-version v2 \
 --docker-registries godaprojects-docker-account  --context $CONTEXT
```
## Enable artifact support
```
hal config features edit --artifacts true
```

## Configure Spinnaker to install in Kubernetes
```
hal config deploy edit --type distributed --account-name goda-eks-spinnaker-2952020
```

## Configure Spinnaker to use AWS S3
```
export YOUR_ACCESS_KEY_ID=<access-key>
hal config storage s3 edit --access-key-id $YOUR_ACCESS_KEY_ID \
 --secret-access-key --region ap-southeast-2
hal config storage edit --type s3
```

## Choose the Spinnaker version
```
hal version list
export VERSION=1.18.12
hal config version edit --version $VERSION
chmod 666 ~/.kube/config
sudo chmod 666 /root/.kube/config
sudo cp -r .kube/ /root/
sudo chmod 777 /root/.kube/*
hal deploy apply
```

## Verify the Spinnaker installation
```
sudo kubectl -n spinnaker get svc
```

## Spinnaker load balancer

```
export NAMESPACE=spinnaker

sudo kubectl -n ${NAMESPACE} expose service spin-gate --type LoadBalancer \
  --port 80 --target-port 8084 --name spin-gate-public 

sudo kubectl -n ${NAMESPACE} expose service spin-deck --type LoadBalancer \
  --port 80 --target-port 9000 --name spin-deck-public  

export API_URL=$(sudo kubectl -n $NAMESPACE get svc spin-gate-public \
 -o jsonpath='{.status.loadBalancer.ingress[0].hostname}') 

export UI_URL=$(sudo kubectl -n $NAMESPACE get svc spin-deck-public -o jsonpath='{.status.loadBalancer.ingress[0].hostname}') 

hal config security api edit --override-base-url http://${API_URL} 

hal config security ui edit --override-base-url http://${UI_URL}

hal deploy apply
```
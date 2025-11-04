# Platform-level cert-manager

1. Apply CRDs once (cluster-scoped):
   kubectl apply -f crds.yaml

2. Create cloudflare token (DNS:Edit) and save:
   kubectl -n cert-manager create secret generic cloudflare-api-token-secret \
   --from-literal=api-token=<YOUR_TOKEN>

3. Install issuer:
   kubectl apply -f issuers/cloudflare-clusterissuer.yaml

4. Check:
   kubectl get clusterissuer cloudflare-clusterissuer
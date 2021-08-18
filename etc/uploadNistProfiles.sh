export TOKEN=mytoken
export K8S_NAMESPACE=daart

kubectl port-forward svc/nist-validator 10031:10031 -n $K8S_NAMESPACE &
echo "forwarding port..."
sleep 10
echo "port forwarded"

export NIST_URL=http://localhost:10031/nist-validator/v1/profiles

profileList=`ls ./profiles/*.zip`
for eachfile in $profileList
do
    xbase=${eachfile##*/}
    profileName=${xbase%.*}

    echo "\nLoading $eachfile as $profileName"
    curl -i -X 'POST' -H "s2s-token:$TOKEN" -F "file=@${eachfile}" $NIST_URL/$profileName
done


echo "killing port forward"
jobs
kill %1

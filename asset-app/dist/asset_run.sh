#!/bin/bash 

function usage() 
{
    echo " Usage : "
    echo "   bash asset_run.sh deploy"
    echo "   bash asset_run.sh query    asset_account "
    echo "   bash asset_run.sh register asset_account asset_amount "
    echo "   bash asset_run.sh transfer from_asset_account to_asset_account amount "
    echo " "
    echo " "
    echo "examples : "
    echo "   bash asset_run.sh deploy "
    echo "   bash asset_run.sh register  Asset0  10000000 "
    echo "   bash asset_run.sh register  Asset1  10000000 "
    echo "   bash asset_run.sh transfer  Asset0  Asset1 11111 "
    echo "   bash asset_run.sh query Asset0"
    echo "   bash asset_run.sh query Asset1"
    exit 0
}

    case $1 in
    deploy)
            [ $# -lt 1 ] && { usage; }
            ;;
    registerCompany)
            [ $# -lt 2 ] && { usage; }
            ;;
    updateReceipt)
            [ $# -lt 4 ] && { usage; }
            ;;
    queryCompany)
            [ $# -lt 2 ] && { usage; }
            ;;
    computeAllR)
            [ $# -lt 2 ] && { usage; }
            ;;
    queryReceipt)
            [ $# -lt 3 ] && { usage; }
            ;;
    skipTime)
            [ $# -lt 4 ] && { usage; }
            ;;
    deleteReceipt)
            [ $# -lt 3 ] && { usage; }
            ;;
    createReceipt)
            [ $# -lt 5 ] && { usage; }
            ;;
    transferReceipt)
            [ $# -lt 5 ] && { usage; }
            ;;
    *)
        usage
            ;;
    esac

    java -Djdk.tls.namedGroups="secp256k1" -cp 'apps/*:conf/:lib/*' org.fisco.bcos.asset.client.AssetClient $@


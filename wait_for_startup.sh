#!/bin/bash

echo "Waiting for CMS Flowable Application to start..."
echo "Base URL: http://localhost:8080/api"
echo ""

max_attempts=60
attempt=1

while [ $attempt -le $max_attempts ]; do
    echo -n "Attempt $attempt/$max_attempts: "
    
    if curl -f http://localhost:8080/api/actuator/health >/dev/null 2>&1; then
        echo "Application is ready!"
        echo ""
        echo "Access Points:"
        echo "  • Swagger UI: http://localhost:8080/api/swagger-ui/index.html"
        echo "  • API Docs: http://localhost:8080/api/v3/api-docs"
        echo "  • Health Check: http://localhost:8080/api/actuator/health"
        echo ""
        echo "Testing Files Created:"
        echo "  • Postman Collection: CMS_Flowable_API_Tests.postman_collection.json"
        echo "  • Postman Environment: CMS_Flowable_Environment.postman_environment.json"
        echo "  • Testing Guide: TESTING_GUIDE.md"
        echo ""
        exit 0
    else
        echo "Not ready yet..."
        sleep 5
        ((attempt++))
    fi
done

echo "Application did not start within 5 minutes"
echo "Please check the console output for any errors"
exit 1
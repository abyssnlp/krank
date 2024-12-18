package com.github.abyssnlp.resourcemanager

import com.github.abyssnlp.common.KrankExceptions
import com.github.abyssnlp.common.ResourceType
import com.github.abyssnlp.common.ScalingConfiguration
import com.github.abyssnlp.common.TargetResource
import io.fabric8.kubernetes.api.model.KubernetesResource
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.StatefulSet
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import org.slf4j.LoggerFactory

class KubernetesResourceManager : ResourceManager<KubernetesResource> {

    private val k8sClient: KubernetesClient =
        KubernetesClientBuilder().build()

    override suspend fun getCurrentReplicas(targetResource: KubernetesResource): Int {
        return when (targetResource) {
            is Deployment -> targetResource.spec.replicas
            is StatefulSet -> targetResource.spec.replicas
            else -> throw KrankExceptions.ResourceTypeNotSupportedException(
                "Resource type ${targetResource::class.java.simpleName} not supported"
            )
        }
    }

    override suspend fun scale(
        targetResource: TargetResource,
        scaleFactor: Int,
        scalingConfig: ScalingConfiguration
    ): Int {
        val resource = when (targetResource.resourceType) {
            ResourceType.DEPLOYMENT -> k8sClient.apps().deployments()
                .inNamespace(targetResource.namespace)
                .withName(targetResource.deploymentName)

            ResourceType.STATEFUL_SET -> k8sClient.apps().statefulSets()
                .inNamespace(targetResource.namespace)
                .withName(targetResource.deploymentName)
        }

        val currentReplicas = getCurrentReplicas(resource.get())
        val newReplicaCount = currentReplicas + scaleFactor
        val finalReplicaCount = newReplicaCount.coerceIn(
            scalingConfig.minReplicas,
            scalingConfig.maxReplicas
        )

        if (finalReplicaCount != currentReplicas) {
            resource.scale(finalReplicaCount)
            logger.info(
                "Scaled {}.{} from {} to {} replicas",
                targetResource.namespace,
                targetResource.deploymentName,
                currentReplicas,
                finalReplicaCount
            )
        } else {
            logger.info(
                "No scaling required for {}.{}. Current replicas: {}," +
                        "Min replicas: {}, Max replicas: {}",
                targetResource.namespace,
                targetResource.deploymentName,
                currentReplicas,
                scalingConfig.minReplicas,
                scalingConfig.maxReplicas
            )
        }
        return finalReplicaCount
    }

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesResourceManager::class.java)
    }
}
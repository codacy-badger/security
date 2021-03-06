package io.github.jokoframework.security.services.impl;

import io.github.jokoframework.common.JokoUtils;
import io.github.jokoframework.security.dto.ConsumerAPIDTO;
import io.github.jokoframework.security.entities.ConsumerApiEntity;
import io.github.jokoframework.security.entities.ConsumerApiEntity.ACCESS_LEVEL;
import io.github.jokoframework.security.errors.JokoConsumerException;
import io.github.jokoframework.security.repositories.IConsumerRepository;
import io.github.jokoframework.security.services.IConsumerAPIService;
import io.github.jokoframework.security.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ConsumerAPIAccessServiceImpl implements IConsumerAPIService {

    private static final int CONSUMER_ID_LENGTH = 15;
    private static final int CONSUMER_SECRET_LENGH = 60;

    @Autowired
    private IConsumerRepository repository;

    @Override
    public ConsumerAPIDTO getConsumer(String consumerId) {
        ConsumerApiEntity entity = repository.getUserApiAccessByConsumerId(consumerId);
        return (ConsumerAPIDTO) entity.toDTO();

    }

    @Override
    public ConsumerAPIDTO generateAndStoreConsumer(ConsumerAPIDTO consumer) throws JokoConsumerException {
        if (consumer.getAccessLevel() == null) {
            throw new JokoConsumerException(JokoConsumerException.MISSING_REQUIRED_DATA,
                    "Missing required field accessLevel");
        } else if (consumer.getName() == null) {
            throw new JokoConsumerException(JokoConsumerException.MISSING_REQUIRED_DATA,
                    "Missing required field \"name\" ");
        }
        
        try {
        	ACCESS_LEVEL.valueOf(consumer.getAccessLevel());
        } catch (IllegalArgumentException e) {
            throw new JokoConsumerException(e, JokoConsumerException.INVALID_ACESS_LEVEL,
                    "Invalid access level. Use one of: PDV, BANK, ON_BEHALF_USER, ATM");
        }

        String consumerId = JokoUtils.generateRandomString(CONSUMER_ID_LENGTH);
        String secret = JokoUtils.generateRandomString(CONSUMER_SECRET_LENGH);

        // Crea el entity para guardar en base al DTO
        ConsumerApiEntity entity = new ConsumerApiEntity();
        entity.setConsumerId(consumerId);
        entity.setSecret(SecurityUtils.hashPassword(secret));
        entity.setName(consumer.getName());
        entity.setContactName(consumer.getContactName());
        entity.setAccessLevel(ACCESS_LEVEL.valueOf(consumer.getAccessLevel()));
        ConsumerApiEntity storedUser = repository.save(entity);
        ConsumerAPIDTO dto = (ConsumerAPIDTO) storedUser.toDTO();
        // Devela el secret en el momento de generar
        dto.setSecret(secret);
        return dto;
    }
    
    @Override
    public List<ConsumerAPIDTO> list() {
        List<ConsumerApiEntity> entities = repository.findAll();
        List<ConsumerAPIDTO> list = JokoUtils.fromEntityToDTO(entities);
        return list;
    }

    @Override
    public boolean isValid(String consumerId, String rawPassword) {
        ConsumerApiEntity entity = repository.getUserApiAccessByConsumerId(consumerId);
        return entity != null && SecurityUtils.matchPassword(rawPassword, entity.getSecret());
    }

    @Override
    public ConsumerAPIDTO changePassword(String consumerId) {
        ConsumerApiEntity entity = repository.getUserApiAccessByConsumerId(consumerId);
        if (entity == null) {
            return null;
        }
        // Genera un nuevo password y guarda encriptado
        String secret = JokoUtils.generateRandomString(CONSUMER_SECRET_LENGH);
        entity.setSecret(SecurityUtils.hashPassword(secret));
        ConsumerApiEntity saved = repository.save(entity);

        ConsumerAPIDTO dto = (ConsumerAPIDTO) saved.toDTO();
        dto.setSecret(secret);

        // Devela el secret en el momento de generar
        return dto;
    }

}

package org.springframework.samples.petclinic.service.clinicService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.repository.OwnerRepository;
import org.springframework.samples.petclinic.repository.PetRepository;
import org.springframework.samples.petclinic.repository.PetTypeRepository;
import org.springframework.samples.petclinic.repository.SpecialtyRepository;
import org.springframework.samples.petclinic.repository.VetRepository;
import org.springframework.samples.petclinic.repository.VisitRepository;
import org.springframework.samples.petclinic.service.ClinicServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClinicServicePetTypeNotFoundTests {

    @Mock
    private PetRepository petRepository;

    @Mock
    private VetRepository vetRepository;

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private SpecialtyRepository specialtyRepository;

    @Mock
    private PetTypeRepository petTypeRepository;

    private ClinicServiceImpl clinicService;

    @BeforeEach
    void setUp() {
        clinicService = new ClinicServiceImpl(petRepository, vetRepository, ownerRepository,
            visitRepository, specialtyRepository, petTypeRepository);
    }

    @Test
    void shouldReturnNullWhenPetTypeRetrievalFails() {
        given(petTypeRepository.findById(999))
            .willThrow(new ObjectRetrievalFailureException(PetType.class, 999));

        PetType result = clinicService.findPetTypeById(999);

        assertThat(result).isNull();
        verify(petTypeRepository).findById(999);
    }

    @Test
    void shouldReturnNullWhenPetTypeQueryHasNoResult() {
        given(petTypeRepository.findById(999))
            .willThrow(new EmptyResultDataAccessException(1));

        PetType result = clinicService.findPetTypeById(999);

        assertThat(result).isNull();
        verify(petTypeRepository).findById(999);
    }

    @Test
    void shouldPropagatePetTypeResourceFailure() {
        DataAccessResourceFailureException failure =
            new DataAccessResourceFailureException("Database resource unavailable");
        given(petTypeRepository.findById(999)).willThrow(failure);

        DataAccessResourceFailureException actual = assertThrows(
            DataAccessResourceFailureException.class,
            () -> clinicService.findPetTypeById(999));

        assertThat(actual).isSameAs(failure);
        verify(petTypeRepository).findById(999);
    }
}

package io.mosip.authentication.otp.service.controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.mosip.authentication.common.service.helper.AuditHelper;
import io.mosip.authentication.common.service.validator.OTPRequestValidator;
import io.mosip.authentication.core.constant.AuditEvents;
import io.mosip.authentication.core.constant.AuditModules;
import io.mosip.authentication.core.constant.IdAuthCommonConstants;
import io.mosip.authentication.core.constant.IdAuthenticationErrorConstants;
import io.mosip.authentication.core.dto.DataValidationUtil;
import io.mosip.authentication.core.exception.IDDataValidationException;
import io.mosip.authentication.core.exception.IdAuthenticationAppException;
import io.mosip.authentication.core.exception.IdAuthenticationBusinessException;
import io.mosip.authentication.core.indauth.dto.IdType;
import io.mosip.authentication.core.logger.IdaLogger;
import io.mosip.authentication.core.otp.dto.OtpRequestDTO;
import io.mosip.authentication.core.otp.dto.OtpResponseDTO;
import io.mosip.authentication.core.spi.otp.service.OTPService;
import io.mosip.kernel.core.logger.spi.Logger;
import springfox.documentation.annotations.ApiIgnore;

/**
 * The {@code OTPAuthController} use to send request to generate otp.
 * 
 * @author Rakesh Roshan
 */
@RestController
public class OTPController {

	private static final String GENERATE_OTP = "generateOTP";

	private static Logger logger = IdaLogger.getLogger(OTPController.class);

	@Autowired
	private OTPService otpService;

	@Autowired
	private OTPRequestValidator otpRequestValidator;
	
	/** The AuditHelper */
	@Autowired
	private AuditHelper auditHelper;

	@InitBinder
	private void initBinder(WebDataBinder binder) {
		binder.setValidator(otpRequestValidator);
	}

	/**
	 * send OtpRequestDTO request to generate OTP and received OtpResponseDTO as
	 * output.
	 *
	 * @param otpRequestDto as request body
	 * @param errors        associate error
	 * @param partnerId the partner id
	 * @param mispLK the misp LK
	 * @return otpResponseDTO
	 * @throws IdAuthenticationAppException the id authentication app exception
	 * @throws IDDataValidationException the ID data validation exception
	 */
	@PostMapping(path = "/{MISP-LK}/{Auth-Partner-ID}/{API-Key}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public OtpResponseDTO generateOTP(@Valid @RequestBody OtpRequestDTO otpRequestDto, @ApiIgnore Errors errors,
			@PathVariable("MISP-LK") String mispLK,@PathVariable("Auth-Partner-ID") String partnerId, @PathVariable("API-Key") String apiKey )
			throws IdAuthenticationAppException, IDDataValidationException {
		OtpResponseDTO otpResponseDTO = null;
		try {
			DataValidationUtil.validate(errors);
			otpResponseDTO = otpService.generateOtp(otpRequestDto, partnerId);
			logger.info(IdAuthCommonConstants.SESSION_ID, this.getClass().getSimpleName(), GENERATE_OTP,
					otpResponseDTO.getResponseTime());
			
			boolean status = otpResponseDTO.getErrors() == null || otpResponseDTO.getErrors().isEmpty();
			auditHelper.audit(AuditModules.OTP_REQUEST, AuditEvents.OTP_TRIGGER_REQUEST_RESPONSE, otpRequestDto.getIndividualId(),
					IdType.getIDTypeOrDefault(otpRequestDto.getIndividualIdType()), "otpRequest status : " + status);
			return otpResponseDTO;
		} catch (IDDataValidationException e) {
			logger.error(IdAuthCommonConstants.SESSION_ID, this.getClass().getSimpleName(), GENERATE_OTP,
					e.getErrorText());
			auditHelper.audit(AuditModules.OTP_REQUEST,  AuditEvents.OTP_TRIGGER_REQUEST_RESPONSE , otpRequestDto.getIndividualId(),
					IdType.getIDTypeOrDefault(otpRequestDto.getIndividualIdType()), e);
			throw new IdAuthenticationAppException(IdAuthenticationErrorConstants.DATA_VALIDATION_FAILED, e);
		} catch (IdAuthenticationBusinessException e) {
			logger.error(IdAuthCommonConstants.SESSION_ID, e.getClass().toString(), e.getErrorCode(), e.getErrorText());
			auditHelper.audit(AuditModules.OTP_REQUEST,  AuditEvents.OTP_TRIGGER_REQUEST_RESPONSE , otpRequestDto.getIndividualId(),
					IdType.getIDTypeOrDefault(otpRequestDto.getIndividualIdType()), e);
			throw new IdAuthenticationAppException(e.getErrorCode(), e.getErrorText(), e);
		}
	}

}
